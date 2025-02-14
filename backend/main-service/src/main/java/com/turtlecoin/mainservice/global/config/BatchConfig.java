package com.turtlecoin.mainservice.global.config;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.netflix.discovery.converters.Auto;
import com.turtlecoin.mainservice.domain.document.dto.DocumentResponseDto;
import com.turtlecoin.mainservice.domain.document.entity.Document;
import com.turtlecoin.mainservice.domain.document.service.ContractService;
import com.turtlecoin.mainservice.domain.document.service.DocumentService;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties
public class BatchConfig {
    @Autowired
    @Qualifier("mainEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ContractService contractService;

    private int chunk = 100;

    @Bean(name = "metaJobRepository")
    public JobRepository jobRepository(@Qualifier("metaDataSource") DataSource metaDataSource, @Qualifier("metaTransactionManager") PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();

        factoryBean.setDatabaseType(DatabaseType.MYSQL.getProductName());
        factoryBean.setIsolationLevelForCreate("ISOLATION_REPEATABLE_READ");
        factoryBean.setDataSource(metaDataSource);
        factoryBean.setTransactionManager(transactionManager);
        factoryBean.afterPropertiesSet();

        return factoryBean.getObject();
    }

    @Bean(name = "metaJobLauncher")
    public JobLauncher jobLauncher(@Qualifier("metaJobRepository") JobRepository jobRepository) {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        return jobLauncher;
    }

    @Bean
    public Job DocUpdateJob(@Qualifier("metaJobRepository") JobRepository jobRepository,
                               Step updateDocumentStep) {
        return new JobBuilder("DocUpdateJob", jobRepository)
                .start(updateDocumentStep)
                .build();
    }

    @Bean
    public Step updateDocumentStep(@Qualifier("metaJobRepository") JobRepository jobRepository,
                                   @Qualifier("mainTransactionManager") PlatformTransactionManager mainTransactionManager){
        return new StepBuilder("updateDocuments", jobRepository)
                .<Document, Document>chunk(chunk, mainTransactionManager)
                .reader(jpaDocumentReader(entityManagerFactory))
                .processor(updateDocumentsProcessor())
                .writer(jpaDocumentWriter(entityManagerFactory))
                .build();
    }

    @Bean
    public JpaPagingItemReader<Document> jpaDocumentReader(EntityManagerFactory entityManagerFactory) {
        JpaPagingItemReader<Document> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT d FROM Document d where d.applicant is null");
        reader.setPageSize(chunk); // chunk 크기와 일치
        return reader;
    }

    @Bean
    public ItemProcessor<Document, Document> updateDocumentsProcessor() {
        return document -> {
            String applicant = "";

            switch(document.getDocType()){
                case BREEDING:
                    applicant = contractService.searchTurtleMultiplicationDocument(document.getTurtleUUID(),
                        document.getDocumentHash()).applicant;
                    break;
                case TRANSFER:
                    applicant = contractService.searchTurtleTransferDocument(document.getTurtleUUID(),
                        document.getDocumentHash()).assignApplicant;
                    break;
                case DEATH:
                    applicant = contractService.searchTurtleDeathDocument(document.getTurtleUUID(),
                        document.getDocumentHash()).applicant;
                    break;
            }
            // 만약 블록체인에서 오류가 생겨서 없는 값인 경우 DB를 지워줘야 함
            if(applicant == null || applicant.isEmpty()){
                documentService.removeDocument(document.getDocumentHash(), document.getTurtleUUID());
                return null;
            }
            else document.assignValuesByBatch(applicant);

            return document;
        };
    }

    @Bean
    public JpaItemWriter<Document> jpaDocumentWriter(EntityManagerFactory entityManagerFactory) {
        JpaItemWriter<Document> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

}
