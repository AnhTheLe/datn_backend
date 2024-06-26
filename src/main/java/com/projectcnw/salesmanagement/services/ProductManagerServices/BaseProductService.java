package com.projectcnw.salesmanagement.services.ProductManagerServices;

import com.projectcnw.salesmanagement.dto.Category.CategoryResponse;
import com.projectcnw.salesmanagement.dto.ResponseObject;
import com.projectcnw.salesmanagement.dto.productDtos.*;
import com.projectcnw.salesmanagement.exceptions.ProductManagerExceptions.ProductException;
import com.projectcnw.salesmanagement.models.Products.BaseProduct;
import com.projectcnw.salesmanagement.models.Products.Category;
import com.projectcnw.salesmanagement.models.Products.Variant;
import com.projectcnw.salesmanagement.repositories.CategoryRepository.CategoryRepository;
import com.projectcnw.salesmanagement.repositories.ProductManagerRepository.BaseProductRepository;
import com.projectcnw.salesmanagement.repositories.ProductManagerRepository.NonJPARepository.NonJPAProductRepository;
import com.projectcnw.salesmanagement.repositories.ProductManagerRepository.VariantRepository;
import com.projectcnw.salesmanagement.services.CategoryServices.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.projectcnw.salesmanagement.utils.Utils.mapListCategoryToListCategoryResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaseProductService {
    private final BaseProductRepository baseProductRepository;

    private final VariantRepository variantRepository;

    private final CategoryService categoryService;

    private final CategoryRepository categoryRepository;

    private final NonJPAProductRepository nonJPAProductRepository;

    private final VariantService variantService;
    private ModelMapper modelMapper = new ModelMapper();


    public List<BaseProductDto> getAll(int page, int size, String query, String categoryIds, String start_date, String end_date, String channels) {
        List<Integer> categoryIdList = categoryIds == null || categoryIds.equals("") ? null : Arrays.asList(categoryIds.split(",")).stream().map(Integer::parseInt).toList();
        List<String> channelList = channels == null || channels.isEmpty() ? null : Arrays.stream(channels.split(",")).toList();
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (start_date != null && !start_date.isEmpty()) {
            try {
                startDate = LocalDate.parse(start_date, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } catch (DateTimeParseException e) {
                log.error("Không thể chuyển đổi start_date thành LocalDateTime", e);
            }
        }

        if (end_date != null && !end_date.isEmpty()) {
            try {
                endDate = LocalDate.parse(end_date, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(LocalTime.MAX);
                ;
            } catch (DateTimeParseException e) {
                log.error("Không thể chuyển đổi start_date thành LocalDateTime", e);
            }
        }

        List<BaseProduct> iBaseProductDtos = nonJPAProductRepository.getAllProduct(page, size, query, categoryIdList, startDate, endDate, channelList);

        List<BaseProductDto> baseProducts = new ArrayList<>();
        for (BaseProduct baseProduct : iBaseProductDtos) {
            BaseProductDto baseProductDto = new BaseProductDto();
            baseProductDto.setId(baseProduct.getId());
            baseProductDto.setName(baseProduct.getName());
            baseProductDto.setLabel(baseProduct.getLabel());
            baseProductDto.setDescription(baseProduct.getDescription());
            baseProductDto.setCreatedAt(baseProduct.getCreatedAt());
            baseProductDto.setVariantNumber(baseProduct.getVariants().size());
            baseProductDto.setQuantity(baseProduct.getVariants().stream().mapToInt(Variant::getQuantity).sum());
            baseProductDto.setVariants(baseProduct.getVariants().stream().map(variant -> modelMapper.map(variant, VariantDto.class)).toList());
            baseProductDto.setListCategories(mapListCategoryToListCategoryResponse(categoryRepository.getListCategoryByProductId(baseProduct.getId())));
            baseProducts.add(baseProductDto);
        }

        return baseProducts;
//        return new PageImpl<>(baseProducts, pageable, baseProducts.size());
    }

    public int countProducts(int page, int size, String query, String categoryIds, String start_date, String end_date, String channels) {
        List<Integer> categoryIdList = categoryIds == null || categoryIds.equals("") ? null : Arrays.asList(categoryIds.split(",")).stream().map(Integer::parseInt).toList();
        List<String> channelList = channels == null || channels.isEmpty() ? null : Arrays.stream(channels.split(",")).toList();
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (start_date != null && !start_date.isEmpty()) {
            try {
                startDate = LocalDate.parse(start_date, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } catch (DateTimeParseException e) {
                log.error("Không thể chuyển đổi start_date thành LocalDateTime", e);
            }
        }

        if (end_date != null && !end_date.isEmpty()) {
            try {
                endDate = LocalDate.parse(end_date, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(LocalTime.MAX);
                ;
            } catch (DateTimeParseException e) {
                log.error("Không thể chuyển đổi start_date thành LocalDateTime", e);
            }
        }

        return nonJPAProductRepository.countProducts(page, size, query, categoryIdList, startDate, endDate, channelList);
    }

    public List<VariantDto> getAllVariantOfBaseProductByBaseId(int baseId) {
        List<IVariantDto> iVariantDtos = baseProductRepository.findAllVariantByBaseProductId(baseId);
        return Arrays.asList(modelMapper.map(iVariantDtos, VariantDto[].class));
    }

    @Transactional
    public BaseProductDto getBaseProductById(int baseId) {
        IBaseProductDto iBaseProductDto = baseProductRepository.findBaseProductById(baseId);
        if (iBaseProductDto == null) throw new ProductException("Không tìm thấy sản phẩm");

        BaseProductDto baseProductDto = modelMapper.map(iBaseProductDto, BaseProductDto.class);
        baseProductDto.setVariants(this.getAllVariantOfBaseProductByBaseId(baseId));
        List<Category> categories = categoryRepository.getListCategoryByIds(categoryRepository.getListCategoryIdByProductId(baseId));
        List<CategoryResponse> listResponse = new ArrayList<>();
        for (Category category : categories) {
            listResponse.add(CategoryResponse.builder().
                    id(category.getId()).
                    title(category.getTitle()).
                    slug(category.getSlug()).
                    description(category.getDescription()).
                    metaTitle(category.getMetaTitle()).
                    productCount(baseProductRepository.countProductByCategory(category.getId())).
                    build());
        }
        baseProductDto.setListCategories(listResponse);
        return baseProductDto;
    }

    public ResponseEntity<ResponseObject> getProductSaleResponse(int baseId) {
        IBaseProductDto iBaseProductDto = baseProductRepository.findBaseProductById(baseId);
        if (iBaseProductDto == null) {
            return ResponseEntity.ok(ResponseObject.builder()
                    .responseCode(404)
                    .message("Không tìm thấy sản phẩm")
                    .build());
        }

        ProductSaleResponse baseProductDto = modelMapper.map(iBaseProductDto, ProductSaleResponse.class);
        List<Variant> iVariantDtos = variantRepository.findVariantsByBaseProductId(baseId);

        List<VariantSaleResponse> variantSaleResponses = variantService.getListVariantResponseFromVariant(iVariantDtos);
        baseProductDto.setVariants(variantSaleResponses);
        baseProductDto.setImages(variantSaleResponses.stream().map(VariantSaleResponse::getImage).toList());
        return ResponseEntity.ok(ResponseObject.builder()
                .responseCode(200)
                .message("Success")
                .data(baseProductDto)
                .build());
    }

    @Transactional
    public BaseProductDto createBaseProduct(BaseProductDto baseProductDto) {
        BaseProduct baseProduct = modelMapper.map(baseProductDto, BaseProduct.class);
        if (baseProductDto.getAttribute1() != null
                && baseProductDto.getAttribute2() != null
                && !baseProductDto.getAttribute2().equals("")
                && baseProductDto.getAttribute1().equals(baseProductDto.getAttribute2())
        ) throw new ProductException("Thuộc tính " + baseProduct.getAttribute1() + " đã tồn tại");
        if (baseProductDto.getAttribute2() != null
                && baseProductDto.getAttribute3() != null
                && !baseProductDto.getAttribute2().equals("")
                && baseProductDto.getAttribute2().equals(baseProductDto.getAttribute3())
        ) throw new ProductException("Thuộc tính " + baseProduct.getAttribute2() + " đã tồn tại");
        if (baseProductDto.getCategoryIds() != null && !baseProductDto.getCategoryIds().isEmpty()) {
            baseProduct.setCategories(categoryRepository.getListCategoryByIds(baseProductDto.getCategoryIds()));
        }
        baseProduct.setVariants(null);
        BaseProduct baseProduct1 = baseProductRepository.save(baseProduct);

//      kiểm tra xem có attribute2, attribute3 không
        boolean isSetAttribute2 = false;
        boolean isSetAttribute3 = false;
        if (baseProduct1.getAttribute2() != null && !baseProduct1.getAttribute2().isBlank()) isSetAttribute2 = true;
        if (baseProduct1.getAttribute3() != null && !baseProduct1.getAttribute3().isBlank()) isSetAttribute3 = true;

        List<Variant> variantList = Arrays.asList(modelMapper.map(baseProductDto.getVariants(), Variant[].class));
        int i = 1;
        ILastIdVariant lastIdVariant = variantRepository.getLastSetId();
        int lastId = 0;
        if (lastIdVariant != null) lastId = lastIdVariant.getLastId();
        for (Variant variant : variantList) {
            variant.setBaseProduct(baseProduct1);
            if (variant.getSku() == null || variant.getSku().equals("")) {
                int tmpSku = baseProduct1.getId() + 100000 + lastId + i;
                String skuString = "SKU" + baseProduct1.getId() + tmpSku;
                variant.setSku(skuString);
            } else if (variant.getSku().startsWith("SKU")) {
                throw new ProductException("Mã SKU không được bắt đầu bằng \"SKU\"");
            }
            if (variant.getBarcode() == null || variant.getBarcode().equals("")) {
                variant.setBarcode(variant.getSku());
            }
            i++;
            if (variant.getValue1() == null || variant.getValue1().isBlank())
                throw new ProductException("Thuộc tính không được để trống");
            if (isSetAttribute2)
                if (variant.getValue2() == null || variant.getValue2().isBlank())
                    throw new ProductException("Thuộc tính không được để trống");
            if (isSetAttribute3)
                if (variant.getValue3() == null || variant.getValue3().isBlank())
                    throw new ProductException("Thuộc tính không được để trống");
        }
        variantRepository.saveAll(variantList);

        return this.getBaseProductById(baseProduct1.getId());
    }

    public void updateProductCategories(@NotNull BaseProduct baseProduct, List<Category> newCategories) {
        // Xóa các liên kết cũ giữa BaseProduct và Category
        for (Category category : baseProduct.getCategories()) {
            category.getProducts().remove(baseProduct);
        }

        // Xóa các liên kết cũ từ BaseProduct
        baseProduct.getCategories().clear();

        // Thêm các liên kết mới giữa BaseProduct và Category
        for (Category category : newCategories) {
            category.getProducts().add(baseProduct);
        }

        // Cập nhật danh sách Category trong BaseProduct
        baseProduct.setCategories(newCategories);

        // Lưu BaseProduct và các thay đổi vào cơ sở dữ liệu
        baseProductRepository.save(baseProduct);
    }


    @Transactional
    public BaseProductDto updateBaseProduct(int baseId, BaseProductDto baseProductDto) {
        BaseProduct baseProduct = baseProductRepository.findBaseProductByIdAndIsDeleted_False(baseId).orElse(null);
        if (baseProduct == null) throw new ProductException("Không tìm thấy sản phẩm");
        if (baseProductDto.getName() == null || baseProductDto.getName().isBlank())
            throw new ProductException("Tên sản phẩm không được trống");

        if (baseProductDto.getCategoryIds() != null && !baseProductDto.getCategoryIds().isEmpty()) {
            updateProductCategories(baseProduct, categoryRepository.getListCategoryByIds(baseProductDto.getCategoryIds()));
        }
        baseProductRepository.updateBaseProduct(baseId, baseProductDto.getName(), baseProductDto.getLabel(), baseProductDto.getDescription());
        BaseProduct baseProduct1 = baseProductRepository.findBaseProductByIdAndIsDeleted_False(baseId).orElse(null);

        BaseProductDto baseProductDto1 = modelMapper.map(baseProduct1, BaseProductDto.class);
        List<Category> categories = categoryRepository.getListCategoryByIds(categoryRepository.getListCategoryIdByProductId(baseId));

        baseProductDto1.setListCategories(mapListCategoryToListCategoryResponse(categories));
        return baseProductDto1;

//        return modelMapper.map(baseProduct1, BaseProductDto.class);
    }

    @Transactional
    public void updateNameAttribute(int baseId, AttributeDto nameAttributeDto) {
        nameAttributeDto.setName(nameAttributeDto.getName().trim());
        BaseProduct baseProduct = baseProductRepository.findById(baseId);
        if (baseProduct == null) throw new ProductException("Không tìm thấy sản phẩm");
        if (nameAttributeDto.getKeyAttribute() == null
                && !nameAttributeDto.getKeyAttribute().equals("attribute1")
                && !nameAttributeDto.getKeyAttribute().equals("attribute2")
                && !nameAttributeDto.getKeyAttribute().equals("attribute3"))
            throw new ProductException("keyAttribute is not null or must be in the array");

        if (nameAttributeDto.getName() == null || nameAttributeDto.getName().isBlank())
            throw new ProductException("name is not null");

        if (nameAttributeDto.getKeyAttribute().equals("attribute1")) {
            if (baseProduct.getAttribute1() == null || baseProduct.getAttribute1().equals(""))
                throw new ProductException("attribute1 is unset");
            if (nameAttributeDto.getName().equals(baseProduct.getAttribute1()))
                throw new ProductException("Thuộc tính '" + nameAttributeDto.getName() + "' đã tồn tại");
            else baseProductRepository.updateNameAttribute1(baseId, nameAttributeDto.getName());
        }
        if (nameAttributeDto.getKeyAttribute().equals("attribute2")) {
            if (baseProduct.getAttribute2() == null || baseProduct.getAttribute2().equals(""))
                throw new ProductException("attribute2 is unset");
            if (nameAttributeDto.getName().equals(baseProduct.getAttribute2()))
                throw new ProductException("Thuộc tính '" + nameAttributeDto.getName() + "' đã tồn tại");
            else baseProductRepository.updateNameAttribute2(baseId, nameAttributeDto.getName());
        }
        if (nameAttributeDto.getKeyAttribute().equals("attribute3")) {
            if (baseProduct.getAttribute3() == null || baseProduct.getAttribute3().equals(""))
                throw new ProductException("attribute3 is unset");
            if (nameAttributeDto.getName().equals(baseProduct.getAttribute3()))
                throw new ProductException("Thuộc tính '" + nameAttributeDto.getName() + "' đã tồn tại");
            else baseProductRepository.updateNameAttribute3(baseId, nameAttributeDto.getName());
        }
    }


    @Transactional
    public void createAttribute(int baseId, AttributeDto attributeDto) {
        attributeDto.setName(attributeDto.getName().trim());
        BaseProduct baseProduct = baseProductRepository.findById(baseId);
        if (baseProduct == null) throw new ProductException("Không tìm thấy sản phẩm");

        if (attributeDto.getName() == null || attributeDto.getName().isBlank())
            throw new ProductException("Tên thuộc tính không được để trống");
        if (attributeDto.getValue() == null || attributeDto.getValue().isBlank())
            throw new ProductException("Giá trị thuộc tính không được trống");

        if (baseProduct.getAttribute1() != null
                && !baseProduct.getAttribute1().equals("")
                && baseProduct.getAttribute2() != null
                && !baseProduct.getAttribute2().equals("")
                && baseProduct.getAttribute3() != null
                && !baseProduct.getAttribute3().equals("")
        )
            throw new ProductException("Đã đủ số lượng thuộc tính cho sản phẩm");

        if (baseProduct.getAttribute2() == null || baseProduct.getAttribute2().equals("")) {
            if (attributeDto.getName().equals(baseProduct.getAttribute1()))
                throw new ProductException("Thuộc tính '" + attributeDto.getName() + "' đã tồn tại");
            baseProductRepository.updateNameAttribute2(baseId, attributeDto.getName());
            baseProductRepository.createValue2AttributeForVariant(baseId, attributeDto.getValue());
        } else if (baseProduct.getAttribute3() == null || baseProduct.getAttribute3().equals("")) {
            if (attributeDto.getName().equals(baseProduct.getAttribute1()) || attributeDto.getName().equals(baseProduct.getAttribute2()))
                throw new ProductException("Thuộc tính '" + attributeDto.getName() + "' đã tồn tại");
            baseProductRepository.updateNameAttribute3(baseId, attributeDto.getName());
            baseProductRepository.createValue3AttributeForVariant(baseId, attributeDto.getValue());
        }

    }

    public VariantDto getVariantById(int variantId) {
        Variant variant = variantRepository.findById(variantId);
        if (variant == null || variant.isDeleted()) throw new ProductException("Không tìm thấy phiên bản");
        return modelMapper.map(variant, VariantDto.class);
    }

    @Transactional
    public void deleteBaseProductAndVariantOfBaseProductByBaseId(int baseId) {
        BaseProduct baseProduct = baseProductRepository.findById(baseId);
        if (baseProduct == null) throw new ProductException("Không tìm thấy sản phẩm");
        baseProductRepository.deleteAllVariantByBaseId(baseId);
        baseProductRepository.deleteBaseProductById(baseId);

    }


    @Transactional
    public void deleteAttributeOfProduct(int baseId, String keyAttribute) {
        if (keyAttribute == null || keyAttribute.isBlank()) throw new ProductException("keyAttribute is not null");
        BaseProduct baseProduct = baseProductRepository.findById(baseId);
        if (baseProduct == null) throw new ProductException("Không tìm thấy sản phẩm");
        if (baseProduct.getAttribute2().equals("")) throw new ProductException("Không thể xóa thuộc tính duy nhất");

        if (keyAttribute.equals("attribute1")) {
            baseProductRepository.updateNameAttribute1(baseId, baseProduct.getAttribute2());
            baseProductRepository.updateNameAttribute2(baseId, baseProduct.getAttribute3());
            baseProductRepository.updateNameAttribute3(baseId, "");

            for (Variant variant : baseProduct.getVariants()) {
//                VariantDto variantDto = modelMapper.map(variant, VariantDto.class);
                variant.setValue1(variant.getValue2());
                variant.setValue2(variant.getValue3());
                variant.setValue3("");
                variantRepository.save(variant);
            }
        } else if (keyAttribute.equals("attribute2")) {
            baseProductRepository.updateNameAttribute2(baseId, baseProduct.getAttribute3());
            baseProductRepository.updateNameAttribute3(baseId, "");

            for (Variant variant : baseProduct.getVariants()) {
//                VariantDto variantDto = modelMapper.map(variant, VariantDto.class);
                variant.setValue2(variant.getValue3());
                variant.setValue3("");
                variantRepository.save(variant);
            }
        } else if (keyAttribute.equals("attribute3")) {
            baseProductRepository.updateNameAttribute3(baseId, "");

            for (Variant variant : baseProduct.getVariants()) {
//                VariantDto variantDto = modelMapper.map(variant, VariantDto.class);
                variant.setValue3("");
                variantRepository.save(variant);
            }
        } else throw new ProductException("keyAttribute is not found");


    }

    @Transactional
    public List<BaseProductDto> getAllBaseProductsByKeyword(String keyword) {
        List<IBaseProductDto> iBaseProductDtos = baseProductRepository.findAllBaseProductsByKeyword(keyword);
        return Arrays.asList(modelMapper.map(iBaseProductDtos, BaseProductDto[].class));
    }

}
