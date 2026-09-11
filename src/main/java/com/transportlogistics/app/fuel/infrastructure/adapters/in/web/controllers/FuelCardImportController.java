package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardImportUseCase;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.FuelCardRequests;
import com.transportlogistics.app.tenancy.CurrentTenant;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/fuel")
public class FuelCardImportController {
    private final FuelCardImportUseCase imports; private final CurrentTenant tenants;
    public FuelCardImportController(FuelCardImportUseCase imports,CurrentTenant tenants){this.imports=imports;this.tenants=tenants;}
    @PostMapping(value="/card-imports",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public FuelCardImportUseCase.Batch upload(@RequestParam UUID providerId,
                                              @RequestPart("file") MultipartFile file,
                                              MultipartHttpServletRequest request)throws IOException{
        if (request.getFileMap().size() != 1 || !request.getFileMap().containsKey("file"))
            throw new IllegalArgumentException("FUEL_CARD_IMPORT_INVALID");
        String contentType = file.getContentType();
        if(contentType==null||!MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType)))
            throw new IllegalArgumentException("FUEL_CARD_IMPORT_INVALID");
        return imports.importJson(context(),providerId,file.getBytes());
    }
    @GetMapping("/card-imports") public List<FuelCardImportUseCase.Batch> batches(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int limit){return imports.batches(context().tenantId(),page,limit);}
    @GetMapping("/card-imports/{id}") public FuelCardImportUseCase.Batch batch(@PathVariable UUID id){return imports.batch(context().tenantId(),id);}
    @GetMapping("/card-transactions") public List<FuelCardImportUseCase.Transaction> transactions(
            @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int limit,
            @RequestParam(required=false)UUID cardId,@RequestParam(required=false)UUID providerId,
            @RequestParam(required=false)java.time.OffsetDateTime from,@RequestParam(required=false)java.time.OffsetDateTime to,
            @RequestParam(required=false)String localStatus,@RequestParam(required=false)String reconciliationStatus,
            @RequestParam(required=false)String indicator,@RequestParam(required=false)Boolean reviewRequired,
            @RequestParam(defaultValue="transactionTimestamp")String sort,@RequestParam(defaultValue="desc")String direction){
        return imports.transactions(context().tenantId(),new FuelCardImportUseCase.TransactionSearch(page,limit,cardId,
                providerId,from,to,localStatus,reconciliationStatus,indicator,reviewRequired,sort,direction));}
    @GetMapping("/card-transactions/{id}") public FuelCardImportUseCase.Transaction transaction(@PathVariable UUID id){return imports.transaction(context().tenantId(),id);}
    @PostMapping("/card-transactions/{id}/match") public FuelCardImportUseCase.Transaction match(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Reconciliation r){return action(id,r,"MATCH");}
    @PostMapping("/card-transactions/{id}/unmatch") public FuelCardImportUseCase.Transaction unmatch(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Reconciliation r){return action(id,r,"UNMATCH");}
    @PostMapping("/card-transactions/{id}/reject") public FuelCardImportUseCase.Transaction reject(@PathVariable UUID id,@Valid @RequestBody FuelCardRequests.Reconciliation r){return action(id,r,"REJECT");}
    private FuelCardImportUseCase.Transaction action(UUID id,FuelCardRequests.Reconciliation r,String action){return imports.reconcile(context(),id,new FuelCardImportUseCase.Action(r.purchaseId(),r.version(),r.reason(),action));}
    private FuelCardImportUseCase.Context context(){var c=tenants.required();return new FuelCardImportUseCase.Context(c.tenantId(),c.actorId());}
}
