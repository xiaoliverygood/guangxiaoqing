package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.Result;
import com.example.common.ResultCode;
import com.example.mapper.ProductMapper;
import com.example.model.dto.PageRequest;
import com.example.model.entity.Favorites;
import com.example.model.entity.Product;
import com.example.model.entity.Wxuser;
import com.example.model.vo.PageVO;
import com.example.model.vo.ProductSmallVo;
import com.example.service.FavoritesService;
import com.example.mapper.FavoritesMapper;
import com.example.utils.AccountHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* @author L
* @description 针对表【favorites】的数据库操作Service实现
* @createDate 2023-08-09 00:55:38
*/
@Service
@Transactional
public class FavoritesServiceImpl extends ServiceImpl<FavoritesMapper, Favorites>
    implements FavoritesService{

    @Resource
    FavoritesMapper favoritesMapper;
    @Resource
    ProductMapper productMapper;

    public Favorites getFavoriteByProductIdAndUserId(String productId, String userId) {
        QueryWrapper<Favorites> favoritesQueryWrapper = new QueryWrapper<>();
        favoritesQueryWrapper.eq("favorite_product_id", productId)
                .eq("favorite_user_id", userId);

        return favoritesMapper.selectOne(favoritesQueryWrapper);
    }
    @Override
    public Result favoriteItems(String productId) {

        Wxuser user = AccountHolder.getUser();

        String favoriteId="favorite:"+ UUID.randomUUID();



        Favorites one = getFavoriteByProductIdAndUserId(productId,user.getId());

        if (one!=null){

            return Result.failure(ResultCode.FAVORITE_ADD_ERROR);

        }

        Favorites favorites = new Favorites();
        favorites.setFavoriteId(favoriteId);
        favorites.setFavoriteProductId(productId);
        favorites.setFavoriteUserId(user.getId());

        save(favorites);

        //更新收藏量
        Product product = productMapper.selectById(productId);
        product.setFavoritesCount(product.getFavoritesCount()+1);
        productMapper.updateById(product);

        return Result.success();
    }

    @Override
    public Result updateFavorite(String productId) {

        Wxuser user = AccountHolder.getUser();

        Favorites one = getFavoriteByProductIdAndUserId(productId,user.getId());

        if (one==null){
            return Result.failure(ResultCode.FAVORITE_DELETE_ERROR);
        }

        favoritesMapper.deleteById(one);
        //更新收藏量
        Product product = productMapper.selectById(productId);
        product.setFavoritesCount(product.getFavoritesCount()-1);
        productMapper.updateById(product);

        return Result.success();
    }

    @Override
    public Result getMyFavorite(PageRequest pageRequest) {

        Wxuser user = AccountHolder.getUser();

        //非常值得学习分页查询的地方
        IPage<Product> myFavoritesByUserId = favoritesMapper.getMyFavoritesByUserId(user.getId(), new Page<>(pageRequest.getCurrentPage(), pageRequest.getPageSize()));

        List<Product> records = myFavoritesByUserId.getRecords();

        List<ProductSmallVo> productSmallVos = records.stream()
                .map(product -> new ProductSmallVo(
                        product.getProductId(),
                        product.getProductTitle(),
                        product.getProductPrice(),
                        product.getProductUnit(),
                        product.getFrontImage(),
                        product.getProductDescription(),
                        product.getProductStatus()
                ))
                .collect(Collectors.toList());

        PageVO<Object> objectPageVO = new PageVO<>(productSmallVos, myFavoritesByUserId.getTotal(), myFavoritesByUserId.getSize(), myFavoritesByUserId.getCurrent());


        return Result.success(objectPageVO) ;
    }

    /**
     * 判断是否收藏
     * @param productId
     * @param userId
     * @return
     */
    @Override
    public Integer judgeFavorite(String productId, String userId) {

        Favorites favoriteByProductIdAndUserId = getFavoriteByProductIdAndUserId(productId, userId);

        if (favoriteByProductIdAndUserId==null){
            return 0;
        }

        return 1;
    }

    @Override
    public Boolean deleteFavoriteByProductId(String productId) {
        QueryWrapper<Favorites> favoriteProductId = new QueryWrapper<Favorites>().eq("favorite_product_id", productId);

        favoritesMapper.delete(favoriteProductId);

        return Boolean.TRUE;
    }
}




