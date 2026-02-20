package com.sindoflow.ops.production;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class StationShiftId implements Serializable {

    private UUID stationId;
    private UUID shiftId;

    public StationShiftId(UUID stationId, UUID shiftId) {
        this.stationId = stationId;
        this.shiftId = shiftId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationShiftId that = (StationShiftId) o;
        return Objects.equals(stationId, that.stationId) && Objects.equals(shiftId, that.shiftId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationId, shiftId);
    }
}
