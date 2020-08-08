package com.thoughtworks.rslist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "trade")
public class TradeDto {
    @Id
    @GeneratedValue
    private int id;
    private double amount;
    private int rank;
    @ManyToOne
    private RsEventDto rsEvent;
}
