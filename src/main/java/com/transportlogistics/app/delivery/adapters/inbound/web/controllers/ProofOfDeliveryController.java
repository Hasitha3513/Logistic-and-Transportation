package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.*;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.*;
import com.transportlogistics.app.delivery.adapters.inbound.web.mappers.*;
import com.transportlogistics.app.delivery.domain.model.PodEvidenceType;
import com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries/{deliveryId}/proof")
public class ProofOfDeliveryController {
    private final ProofOfDeliveryUseCase proofs; private final ProofOfDeliveryWebMapper mapper; private final DeliveryOrderWebMapper orders;
    public ProofOfDeliveryController(ProofOfDeliveryUseCase proofs, ProofOfDeliveryWebMapper mapper, DeliveryOrderWebMapper orders) { this.proofs = proofs; this.mapper = mapper; this.orders = orders; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProofOfDeliveryResponse create(@PathVariable UUID deliveryId, @Valid @RequestBody CreateProofOfDeliveryRequest request, Principal principal) {
        return mapper.toResponse(proofs.create(deliveryId, new ProofOfDeliveryUseCase.CreateCommand(request.deliveryVersion(), request.deviceCapturedAt(), request.latitude(), request.longitude(), request.accuracyMeters(), request.signerName(), request.signerRelationship()), actor(principal)));
    }
    @GetMapping public ProofOfDeliveryResponse get(@PathVariable UUID deliveryId) { return mapper.toResponse(proofs.get(deliveryId)); }
    @PostMapping(value="/evidence", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public ProofOfDeliveryResponse add(@PathVariable UUID deliveryId, @RequestParam long podVersion,
            @RequestParam PodEvidenceType type, @RequestParam(required=false) String barcodeValue,
            @RequestParam(required=false) String captureSource, @RequestPart(required=false) MultipartFile file,
            Principal principal) throws IOException {
        byte[] content = file == null ? null : file.getBytes(); String filename = file == null ? null : file.getOriginalFilename();
        return mapper.toResponse(proofs.addEvidence(deliveryId, new ProofOfDeliveryUseCase.AddEvidenceCommand(podVersion, type, content, filename, barcodeValue, captureSource), actor(principal)));
    }
    @PostMapping("/finalize")
    public ProofFinalizationResponse finalizeProof(@PathVariable UUID deliveryId, @Valid @RequestBody FinalizeProofOfDeliveryRequest request, Principal principal) {
        var result = proofs.finalizeProof(deliveryId, new ProofOfDeliveryUseCase.FinalizeCommand(request.deliveryVersion(), request.podVersion()), actor(principal));
        return new ProofFinalizationResponse(mapper.toResponse(result.proof()), orders.toResponse(result.delivery()));
    }
    @DeleteMapping("/evidence/{evidenceId}")
    public ProofOfDeliveryResponse remove(@PathVariable UUID deliveryId, @PathVariable UUID evidenceId,
                                          @RequestParam long podVersion, Principal principal) {
        return mapper.toResponse(proofs.removeEvidence(deliveryId, evidenceId, podVersion, actor(principal)));
    }
    @GetMapping("/evidence/{evidenceId}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID deliveryId, @PathVariable UUID evidenceId) {
        var content = proofs.content(deliveryId, evidenceId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.contentLength()).header(HttpHeaders.CONTENT_DISPOSITION, "inline").body(content.content());
    }
    private String actor(Principal principal) { return PrincipalUtils.resolveActorName(principal, null); }
}
