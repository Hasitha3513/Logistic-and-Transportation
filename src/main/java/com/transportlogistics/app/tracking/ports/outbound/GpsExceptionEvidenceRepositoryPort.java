package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;

public interface GpsExceptionEvidenceRepositoryPort {
    boolean append(GpsExceptionEvidence evidence);
}
