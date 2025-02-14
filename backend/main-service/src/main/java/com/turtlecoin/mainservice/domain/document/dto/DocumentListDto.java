package com.turtlecoin.mainservice.domain.document.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentListDto {
	private String docType;
	private String scientificName;
	private String name;
	private String email;
	private LocalDate registerDate;
	private String turtleUUID;
	private String documentHash;
}
