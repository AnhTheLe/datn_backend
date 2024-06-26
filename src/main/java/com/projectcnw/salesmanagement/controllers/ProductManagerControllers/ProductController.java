package com.projectcnw.salesmanagement.controllers.ProductManagerControllers;

import com.projectcnw.salesmanagement.controllers.BaseController;
import com.projectcnw.salesmanagement.dto.PagedResponseObject;
import com.projectcnw.salesmanagement.dto.ResponseObject;
import com.projectcnw.salesmanagement.dto.SalesChannelDTO.PublishProductDTO;
import com.projectcnw.salesmanagement.dto.productDtos.AttributeDto;
import com.projectcnw.salesmanagement.dto.productDtos.BaseProductDto;
import com.projectcnw.salesmanagement.dto.productDtos.VariantDto;
import com.projectcnw.salesmanagement.repositories.ProductManagerRepository.NonJPARepository.impl.NonJpaProductRepository;
import com.projectcnw.salesmanagement.services.BaseProductSalesChannelService.BaseProductSalesChannelService;
import com.projectcnw.salesmanagement.services.ProductManagerServices.BaseProductService;
import com.projectcnw.salesmanagement.services.ProductManagerServices.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ProductController extends BaseController {
    private final BaseProductService baseProductService;
    private final NonJpaProductRepository nonJpaProductRepository;
    private final VariantService variantService;
    private final BaseProductSalesChannelService baseProductSalesChannelService;

    //
    //viewListProducts
    //lấy danh sách sản phẩm (gồm tồn kho, số phiên bản)
    @GetMapping("/base-products")
    public ResponseEntity<PagedResponseObject> getAllBaseProduct(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                 @RequestParam(name = "size", defaultValue = "10") int size,
                                                                 @RequestParam(name = "query", defaultValue = "") String query,
                                                                 @RequestParam(name = "categoryIds", defaultValue = "") String categoryIds,
                                                                 @RequestParam(name = "startDate", defaultValue = "") String startDate,
                                                                 @RequestParam(name = "endDate", defaultValue = "") String endDate,
                                                                 @RequestParam(name = "channel", defaultValue = "") String channels
    ) {
        List<BaseProductDto> products = baseProductService.getAll(page, size, query, categoryIds, startDate, endDate, channels);
        long totalItems = baseProductService.countProducts(page, size, query, categoryIds, startDate, endDate, channels);
        int totalPages = (int) Math.ceil((double) totalItems / size);
        return ResponseEntity.ok(PagedResponseObject.builder()
                .page(page)
                .perPage(size)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .responseCode(200)
                .message("Success")
                .data(products)
                .build());
    }
//    private int page;
//    private int perPage;
//    private int totalItems;
//    private int totalPages;

    //viewProduct
    //lây chi tiết 1 sản phẩm (gồm cả các phiên bản của nó)
    @GetMapping("/base-products/{id}")
    public ResponseEntity<ResponseObject> getDetailedBaseProductByBaseId(@PathVariable("id") int baseId) {
        BaseProductDto baseProductDto = baseProductService.getBaseProductById(baseId);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(baseProductDto)
                .build());
    }

    //createBaseProduct
    //tạo mới một base-product
    @PostMapping("/base-products")
    public ResponseEntity<ResponseObject> createBaseProduct(@Valid @RequestBody BaseProductDto baseProductDto) {
        BaseProductDto baseProductDto1 = baseProductService.createBaseProduct(baseProductDto);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(baseProductDto1)
                .build());
    }

    @PutMapping("/base-products/{id}")
    public ResponseEntity<ResponseObject> updateBaseProduct(@PathVariable("id") int baseId, @RequestBody BaseProductDto baseProductDto) {
        BaseProductDto baseProductDto1 = baseProductService.updateBaseProduct(baseId, baseProductDto);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(baseProductDto1)
                .build());
    }

    @PostMapping("/base-products/{id}/variants")
    public ResponseEntity<ResponseObject> createVariantOfBaseProduct(@PathVariable("id") int baseId, @Valid @RequestBody VariantDto variantDto) {
        VariantDto variantDto1 = variantService.createVariant(baseId, variantDto);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(variantDto1)
                .build());
    }

    @GetMapping("/base-products/{baseId}/variants/{variantId}")
    public ResponseEntity<ResponseObject> getVariantById(@PathVariable("baseId") int baseId, @PathVariable("variantId") int variantId) {
        VariantDto variantDto = baseProductService.getVariantById(variantId);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(variantDto)
                .build());
    }


    @PutMapping("/base-products/{id}/variants")
    public ResponseEntity<ResponseObject> updateVariantOfBaseProduct(@PathVariable("id") int baseId, @Valid @RequestBody VariantDto variantDto) {
        VariantDto variantDto1 = variantService.updateVariant(baseId, variantDto);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(variantDto1)
                .build());
    }

    @PutMapping("/base-products/{id}/attributes")
    public ResponseEntity<ResponseObject> updateNameAttribute(@PathVariable("id") int baseId, @RequestBody AttributeDto nameAttributeDto) {
        baseProductService.updateNameAttribute(baseId, nameAttributeDto);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(null)
                .build());
    }

    @PostMapping("/base-products/{id}/attributes")
    public ResponseEntity<ResponseObject> createAttribute(@PathVariable("id") int baseId, @RequestBody AttributeDto attributeDto) {
        baseProductService.createAttribute(baseId, attributeDto);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Created Attriute")
                .data(null)
                .build());
    }

    @DeleteMapping("/base-products/{baseId}/attributes")
    public ResponseEntity<ResponseObject> deleteAttribute(@PathVariable("baseId") int baseId, @RequestBody AttributeDto attributeDto) {
        baseProductService.deleteAttributeOfProduct(baseId, attributeDto.getKeyAttribute());
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Deleted")
                .data(null)
                .build());
    }

    @DeleteMapping("/base-products/{id}")
    public ResponseEntity<ResponseObject> deleteBaseProductAndVariantOfBaseProduct(@PathVariable("id") int baseId) {
        baseProductService.deleteBaseProductAndVariantOfBaseProductByBaseId(baseId);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Deleted")
                .data(null)
                .build());
    }

    @DeleteMapping("/base-products/{baseId}/variants/{variantId}")
    public ResponseEntity<ResponseObject> deleteVariantById(@PathVariable("baseId") int baseId, @PathVariable("variantId") int variantId) {
        variantService.deleteVariantById(baseId, variantId);
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Deleted")
                .data(null)
                .build());
    }

    @GetMapping("base-products/search")
    public ResponseEntity<ResponseObject> getAllBaseProductsByKeyword(@RequestParam(name = "keyword") String keyword) {
        List<BaseProductDto> baseProductDtos = baseProductService.getAllBaseProductsByKeyword(keyword);

        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(baseProductDtos)
                .build());
    }

    @PostMapping("/base-products/publish")
    public ResponseEntity<ResponseObject> publishProduct(@RequestBody PublishProductDTO publishProductDTO) {
        return baseProductSalesChannelService.publishProduct(publishProductDTO);
    }

    @GetMapping("/base-products/{productId}/sales-channels")
    public ResponseEntity<ResponseObject> getProductSalesChannels(@PathVariable Integer productId) {
        return baseProductSalesChannelService.getProductSalesChannels(productId);
    }

    @PostMapping("/base-products/publish-all")
    public ResponseEntity<Void> publishAllUnpublishedProducts() {
        baseProductSalesChannelService.publishAllUnpublishedProducts();
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/base-products/{productId}/republish")
    public ResponseEntity<ResponseObject> unpublishProduct(@PathVariable Integer productId, @RequestBody PublishProductDTO publishProductResponseDTO) {
        return baseProductSalesChannelService.updatePublishProduct(productId, publishProductResponseDTO);

    }
}
