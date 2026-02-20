package com.sindoflow.ops.production;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
public class ShiftEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "days_of_week", columnDefinition = "integer[]", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<Integer> daysOfWeek;

    @Column(name = "capacity_hours", precision = 5, scale = 2)
    private BigDecimal capacityHours;
}
