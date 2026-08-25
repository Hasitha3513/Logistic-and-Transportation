package com.transportlogistics.app.freight.manifest.ports.outbound;
import java.util.function.Supplier;
public interface CargoManifestTransaction { <T> T execute(Supplier<T> operation); }
