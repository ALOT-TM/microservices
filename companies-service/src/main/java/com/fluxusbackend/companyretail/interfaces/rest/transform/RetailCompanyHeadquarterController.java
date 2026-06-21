package com.fluxusbackend.companyretail.interfaces.rest.transform;

import com.fluxusbackend.companyretail.domain.model.aggregates.RetailCompanyHeadquarter;
import com.fluxusbackend.companyretail.domain.model.commands.CreateRetailCompanyHeadquarterCommand;
import com.fluxusbackend.companyretail.infrastructure.persistence.jpa.repositories.RetailCompanyHeadquarterRepository;
import com.fluxusbackend.companyretail.infrastructure.persistence.jpa.repositories.RetailCompanyRepository;
import com.fluxusbackend.location.infrastructure.persistence.jpa.repositories.AddressRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retail-company-headquarters")
@Tag(name = "Retail Company Headquarters", description = "Retail company headquarters administration")
public class RetailCompanyHeadquarterController {

    private final RetailCompanyHeadquarterRepository headquarterRepository;
    private final RetailCompanyRepository retailCompanyRepository;
    private final AddressRepository addressRepository;

    public RetailCompanyHeadquarterController(
            RetailCompanyHeadquarterRepository headquarterRepository,
            RetailCompanyRepository retailCompanyRepository,
            AddressRepository addressRepository
    ) {
        this.headquarterRepository = headquarterRepository;
        this.retailCompanyRepository = retailCompanyRepository;
        this.addressRepository = addressRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create retail company headquarter")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Headquarter created",
                    content = @Content(schema = @Schema(implementation = RetailCompanyHeadquarter.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public RetailCompanyHeadquarter create(@Valid @RequestBody CreateRetailCompanyHeadquarterCommand command) {
        var retailCompany = retailCompanyRepository.findById(command.retailCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Retail company not found"));
        var address = addressRepository.findById(command.addressId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        var headquarter = new RetailCompanyHeadquarter(retailCompany, command.description(), address);
        return headquarterRepository.save(headquarter);
    }

    @GetMapping("/{headquarterId}")
    @Operation(summary = "Get retail company headquarter by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Headquarter found",
                    content = @Content(schema = @Schema(implementation = RetailCompanyHeadquarter.class))),
            @ApiResponse(responseCode = "404", description = "Headquarter not found", content = @Content)
    })
    public RetailCompanyHeadquarter getById(@PathVariable Long headquarterId) {
        return headquarterRepository.findById(headquarterId)
                .orElseThrow(() -> new IllegalArgumentException("Headquarter not found"));
    }

    @GetMapping("/{headquarterId}/company-id")
    @Operation(summary = "Get owner company id for retail company headquarter")
    public Long getCompanyIdByHeadquarterId(@PathVariable Long headquarterId) {
        return headquarterRepository.findById(headquarterId)
                .orElseThrow(() -> new IllegalArgumentException("Headquarter not found"))
                .getRetailCompany()
                .getRetailCompanyId();
    }

    @GetMapping
    @Operation(summary = "List retail company headquarters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Headquarters retrieved",
                    content = @Content(schema = @Schema(implementation = RetailCompanyHeadquarter.class)))
    })
    public List<RetailCompanyHeadquarter> list() {
        return headquarterRepository.findAll();
    }

    @PutMapping("/{headquarterId}")
    @Operation(summary = "Update retail company headquarter")
    public RetailCompanyHeadquarter update(@PathVariable Long headquarterId, @Valid @RequestBody UpdateHeadquarterResource resource) {
        var headquarter = headquarterRepository.findById(headquarterId)
                .orElseThrow(() -> new IllegalArgumentException("Headquarter not found"));
        var address = addressRepository.findById(resource.addressId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        headquarter.update(resource.description(), address);
        return headquarterRepository.save(headquarter);
    }

    @DeleteMapping("/{headquarterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete retail company headquarter")
    public void delete(@PathVariable Long headquarterId) {
        var headquarter = headquarterRepository.findById(headquarterId)
                .orElseThrow(() -> new IllegalArgumentException("Headquarter not found"));
        var address = headquarter.getAddress();
        headquarterRepository.delete(headquarter);
        if (address != null) {
            addressRepository.delete(address);
        }
    }

    public record UpdateHeadquarterResource(String description, Long addressId) {}
}

