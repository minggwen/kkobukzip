package com.turtlecoin.mainservice.domain.turtle.entity;

import com.turtlecoin.mainservice.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "turtle_photo")
public class TurtlePhoto extends BaseEntity {
    public TurtlePhoto(Turtle turtle, String image){
        this.turtle = turtle;
        this.image = image;
    }


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turtle_id", nullable = false)
    private Turtle turtle;

    @Column(nullable = false)
    private String image;
}
