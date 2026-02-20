package com.sindoflow.ops.production;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "station_shifts")
@Getter
@Setter
@NoArgsConstructor
public class StationShiftEntity {

    @EmbeddedId
    private StationShiftId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stationId")
    @JoinColumn(name = "station_id")
    private StationEntity station;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("shiftId")
    @JoinColumn(name = "shift_id")
    private ShiftEntity shift;

    public StationShiftEntity(StationEntity station, ShiftEntity shift) {
        this.id = new StationShiftId(station.getId(), shift.getId());
        this.station = station;
        this.shift = shift;
    }
}
