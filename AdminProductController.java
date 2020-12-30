package com.mall.controller.admin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mall.controller.BaseController;
import com.mall.entity.*;
import com.mall.service.*;
import com.mall.util.OrderUtil;
import com.mall.util.PageUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

/**
 * @author one
 * @createTime 2020年12月21日
 */
@Controller
public class AdminProductController extends BaseController {

    @Resource
    private ProductService productService;
    @Resource
    private AdminService adminService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private ProductImageService productImageService;
    @Resource
    private PropertyValueService propertyValueService;
    @Resource
    private PropertyService propertyService;

	//111

    //查询所有产品-ajax
    @RequestMapping(value = "admin/product")
    public String getToPage(HttpSession session, Model model) {
        Object adminId = checkAdmin(session);
        Admin admin;
        if (adminId != null) {
            logger.info("登录成功");
            admin = adminService.get(Integer.parseInt(adminId.toString()));
            model.addAttribute("admin", admin);
        } else {
            return "redirect:/admin/login";
        }
        //获取产品分类
        List<Category> categoryList = categoryService.getList(null, null);
        model.addAttribute("categoryList", categoryList);
        //一页显示十条数据
        PageUtil pageUtil = new PageUtil(0, 10);
        //获取产品
        List<Product> productList = productService.getList(null, null,
                null, pageUtil);
        model.addAttribute("productList", productList);
        //获取产品总数量
        Integer productCount = productService.getTotal(null, null);
        model.addAttribute("productCount", productCount);
        pageUtil.setTotal(productCount);
        model.addAttribute("pageUtil", pageUtil);
        return "admin/productManagePage";
    }

    //按条件查询产品-ajax
    @ResponseBody
    @RequestMapping(value = "admin/product/{index}/{count}", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    public String getProductBySearch(@PathVariable("index") Integer index,
                                     @PathVariable("count") Integer count,
                                     @RequestParam(required = false) String productName, //产品名称
                                     @RequestParam(required = false) Integer categoryId, //产品类型ID
                                     @RequestParam(required = false) Double productSalePrice, //产品最低价
                                     @RequestParam(required = false) Double productPrice, //产品最高价
                                     @RequestParam(required = false) Byte[] productIsEnabledArray, //产品状态数组
                                     @RequestParam(required = false) String orderBy, //排序字段
                                     @RequestParam(required = false, defaultValue = "true") Boolean isDesc /*是否倒序*/) throws Exception {
        if (productIsEnabledArray != null && (productIsEnabledArray.length <= 0 || productIsEnabledArray.length >= 3)) {
            productIsEnabledArray = null;
        }
        if (categoryId != null && categoryId == 0) {
            categoryId = null;
        }
        if (productName != null) {
            //如果为非空字符串则解决中文乱码
            productName = productName.equals("") ? null : URLDecoder.decode(productName, "UTF-8");
        }
        if (orderBy != null && orderBy.equals("")) {
            orderBy = null;
        }
        //封装查询条件
        Product product = new Product();
        Category category = new Category();
        category.setCategoryId(categoryId);
        product.setProductName(productName);
        product.setProductCategory(category);
        product.setProductPrice(productPrice);
        OrderUtil orderUtil = null;
        if (orderBy != null) {
            logger.info("根据{}排序，是否倒序:{}", orderBy, isDesc);
            orderUtil = new OrderUtil(orderBy, isDesc);
        }
        JSONObject object = new JSONObject();
        PageUtil pageUtil = new PageUtil(index, count);
        List<Product> productList = productService.getList(product, productIsEnabledArray, orderUtil, pageUtil);
        object.put("productList", JSONArray.parseArray(JSON.toJSONString(productList)));
        //按条件查询获取产品总数量
        Integer productCount = productService.getTotal(product, productIsEnabledArray);
        object.put("productCount", productCount);
        //获取分页信息
        pageUtil.setTotal(productCount);
        object.put("totalPage", pageUtil.getTotalPage());
        object.put("pageUtil", pageUtil);
        return object.toJSONString();
    }

	//333
	
    //产品详情页-ajax
    @RequestMapping(value = "admin/product/{pid}", method = RequestMethod.GET)
    public String goToDetailsPage(HttpSession session, Model model, @PathVariable Integer pid) {
        logger.info("检查管理员权限");
        Object adminId = checkAdmin(session);
        if (adminId == null) {
            return "redirect:/admin/login";
        }
        logger.info("获取productId为{}的产品信息", pid);
        Product product = productService.get(pid);
        logger.info("获取产品详情-图片信息");
        List<ProductImage> productImageList = productImageService.getList(pid, null, null);
        List<ProductImage> singleProductImageList = new ArrayList<>(5);
        List<ProductImage> detailsProductImageList = new ArrayList<>(8);
        for (ProductImage productImage : productImageList) {
            if (productImage.getProductImageType() == 0) {
                singleProductImageList.add(productImage);
            } else {
                detailsProductImageList.add(productImage);
            }
        }
        product.setSingleProductImageList(singleProductImageList);
        product.setDetailProductImageList(detailsProductImageList);
        model.addAttribute("product", product);
        logger.info("获取产品详情-属性值信息");
        PropertyValue propertyValue = new PropertyValue();
        propertyValue.setPropertyValueProduct(product);
        //根据商品的id去查询属性值表的数据
        List<PropertyValue> propertyValueList = propertyValueService.getList(propertyValue, null);
        logger.info("获取产品详情-分类信息对应的属性列表");
        Property property = new Property();
        property.setPropertyCategory(product.getProductCategory());
        //通过分类id去查询属性表的数据
        List<Property> propertyList = propertyService.getList(property, null);
        logger.info("属性列表和属性值列表合并");
        for (Property propertyEach : propertyList) {
            for (PropertyValue propertyValueEach : propertyValueList) {
                if (propertyEach.getPropertyId().equals(propertyValueEach.getPropertyValueProperty().getPropertyId())) {
                    List<PropertyValue> propertyValueItem = new ArrayList<>(1);
                    propertyValueItem.add(propertyValueEach);
                    propertyEach.setPropertyValueList(propertyValueItem);
                    break;
                }
            }
        }
        model.addAttribute("propertyList", propertyList);
        List<Category> categoryList = categoryService.getList(null, null);
        model.addAttribute("categoryList", categoryList);
        return "admin/include/productDetails";
    }

    //删除产品详情页的图片
    @ResponseBody
    @RequestMapping(value = "admin/productImage/{productImageId}", method = RequestMethod.DELETE, produces = "application/json;charset=utf-8")
    public String deleteProductImageId(@PathVariable Integer productImageId/* 产品图片id */) {
        JSONObject object = new JSONObject();
        Boolean yn = productImageService.deleteList(new Integer[]{productImageId});
        if (yn) {
            logger.info("删除成功");
            object.put("success", true);
        }
        return object.toJSONString();
    }

    //更新产品信息
    @ResponseBody
    @RequestMapping(value = "admin/product/{productId}",method = RequestMethod.PUT,produces = "application/json;charset=utf-8")
    public String updateProduct(@RequestParam String productName,
                                @RequestParam String productTitle,
                                @RequestParam Integer productCategoryId,
                                @RequestParam Double productSalePrice /* 产品最低价*/,
                                @RequestParam Double productPrice /* 产品最高价 */,
                                @RequestParam Byte productIsEnabled /* 产品状态*/,
                                @RequestParam String propertyAddJson /* 产品添加属性JSON */,
                                @RequestParam String propertyUpdateJson /* 产品更新属性JSON */,
                                @RequestParam(required = false) Integer[] propertyDeleteList /* 产品删除属性 */,
                                @RequestParam(required = false) String[] productSingleImageList /* 产品预览图片*/,
                                @RequestParam(required = false) String[] productDetailsImageList /* 产品详细图片*/,
                                @PathVariable(value = "productId") Integer productId){
        JSONObject jsonObject=new JSONObject();
        logger.info("整合产品信息");
        Category category=new Category();
        category.setCategoryId(productCategoryId);
        Product product=new Product(productId,productName,productTitle,category,productSalePrice,productPrice,productIsEnabled,new Date());
        logger.info("更新产品信息，产品ID值为：{}",productId);
        boolean flag=productService.update(product);
        if(!flag){
            logger.info("产品信息更新失败！事务回滚");
            jsonObject.put("success",false);
            throw new RuntimeException();
        }
        logger.info("产品信息更新成功！！！");

        JSONObject object=JSON.parseObject(propertyAddJson);
        //取出所有的键名
        Set<String> propertyIdSet=object.keySet();
        if(propertyIdSet.size()>0){
            logger.info("整合产品子信息-需要添加的产品属性");
            List<PropertyValue> propertyValueList=new ArrayList<>(5);
            for (String key : propertyIdSet) {
                Property property=new Property();
                //设置属性id (key就是每一个属性的id)
                property.setPropertyId(Integer.parseInt(key));
                //通过键名取出属性值
                String value=object.getString(key.toString());
                PropertyValue propertyValue=new PropertyValue(value,property,product);
                propertyValueList.add(propertyValue);
            }
            logger.info("共有{}条需要添加的产品属性数据",propertyValueList.size());
            flag=propertyValueService.addList(propertyValueList);
            if(flag){
                logger.info("产品属性添加成功");
            }else{
                logger.info("产品属性添加失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        object=JSON.parseObject(propertyUpdateJson);
        propertyIdSet=object.keySet();
        if(propertyIdSet.size()>0){
            logger.info("整合产品子信息-需要更新的产品属性");
            List<PropertyValue> propertyValueList=new ArrayList<>(5);
            for (String key : propertyIdSet) {
                Property property=new Property();
                //通过键名取出属性值
                String value=object.getString(key.toString());
                PropertyValue propertyValue=new PropertyValue(value,Integer.parseInt(key));
                propertyValueList.add(propertyValue);
            }
            logger.info("共有{}条需要更新的产品属性数据",propertyValueList.size());
            for(int i=0;i<propertyValueList.size();i++){
                logger.info("正在更新第{}条，共{}条",i+1,propertyValueList.size());
                flag=propertyValueService.update(propertyValueList.get(i));
                if(flag){
                    logger.info("产品属性更新成功");
                }else{
                    logger.info("产品属性更新失败！事务回滚");
                    jsonObject.put("success",false);
                    throw new RuntimeException();
                }
            }
        }
        if(propertyDeleteList !=null && propertyDeleteList.length>0){
            logger.info("整合产品子信息-需要删除的产品属性");
            logger.info("共有{}条需要删除的产品属性数据",propertyDeleteList.length);
            flag=propertyValueService.deleteList(propertyDeleteList);
            if(flag){
                logger.info("产品属性删除成功");
            }else{
                logger.info("产品属性删除失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        if(productSingleImageList !=null && productSingleImageList.length>0){
            logger.info("整合产品子信息-产品预览图片");
            List<ProductImage> productImageList=new ArrayList<>(5);
            for (String imageName : productSingleImageList) {
                ProductImage productImage=new ProductImage();
                productImage.setProductImageSrc(imageName.substring(imageName.lastIndexOf("/")+1));
                productImage.setProductImageType((byte)0);
                productImage.setProductImageProduct(product);
                productImageList.add(productImage);
            }
            logger.info("共有{}条产品预览图片数据",productImageList.size());
            flag=productImageService.addList(productImageList);
            if(flag){
                logger.info("产品预览图片添加成功");
            }else{
                logger.info("产品预览图片添加失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        if(productDetailsImageList!=null &&productDetailsImageList.length>0){
            logger.info("整合产品子信息-产品详情图片");
            List<ProductImage> productImageList=new ArrayList<>(5);
            for (String imageName : productDetailsImageList) {
                ProductImage productImage=new ProductImage();
                productImage.setProductImageSrc(imageName.substring(imageName.lastIndexOf("/")+1));
                productImage.setProductImageType((byte)0);
                productImage.setProductImageProduct(product);
                productImageList.add(productImage);
            }
            logger.info("共有{}条产品详情图片数据",productImageList.size());
            flag=productImageService.addList(productImageList);
            if(flag){
                logger.info("产品详情图片添加成功");
            }else{
                logger.info("产品详情图片添加失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        jsonObject.put("success",true);
        jsonObject.put("productId",productId);
        return jsonObject.toJSONString();
    }

    //上传产品图片-ajax
    @ResponseBody
    @RequestMapping(value = "admin/uploadProductImage",method = RequestMethod.POST,produces = "application/json;charset=utf-8")
    public String uploadProductImage(@RequestParam MultipartFile file,@RequestParam String imageType,HttpSession session){
        String originalFilename=file.getOriginalFilename();
        logger.info("获取图片原始文件名：{}",originalFilename);
        String extension=originalFilename.substring(originalFilename.lastIndexOf('.'));
        String filePath;
        String fileName= UUID.randomUUID() + extension;
        if(imageType.equals("single")){
            filePath=session.getServletContext().getRealPath("/")+
                    "res/images/item/productSinglePicture" + fileName;
        }else{
            filePath=session.getServletContext().getRealPath("/")+
                    "res/images/item/productDetailsPicture" + fileName;
        }
        logger.info("文件上传路径：{}",filePath);
        JSONObject object=new JSONObject();
        try {
            logger.info("文件正在上传中");
            file.transferTo(new File(filePath));
            logger.info("文件上传完成");
            object.put("success",true);
            object.put("fileName",fileName);
        }catch (IOException e){
            logger.warn("文件上传失败！");
            e.printStackTrace();
            object.put("success",false);
        }
        return object.toJSONString();
    }

}
