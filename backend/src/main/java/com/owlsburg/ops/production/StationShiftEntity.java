package com.owlsburg.ops.production;

import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "station_shifts")
@Getter
@Setter
@NoArgsConstructor
public class StationShiftEntity {

    @EmbeddedId
    private StationShiftId id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

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

    @PrePersist
    protected void prePersistTenant() {
        if (tenantId == null) {
            String tid = TenantContext.getCurrentTenant();
            if (tid != null) {
                tenantId = UUID.fromString(tid);
            }
        }
    }
}
