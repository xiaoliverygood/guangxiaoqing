package com.example.controller;

import com.example.common.Result;
import com.example.service.FileUploadDelete;
import com.example.utils.AccountHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 文件上传删除
 */
@RestController
@RequestMapping("/upload")
public class FileUploadDeleteController {

    @Resource
    FileUploadDelete fileUploadDelete;

    /**
     * 上传帖子的文件接口
     * @param file
     * @return
     */
    @PostMapping("/MdForum")
    public String uploadMd(@NotNull MultipartFile file){

        return fileUploadDelete.uploadMd(file,"ForumMd:");
    }

    /**
     * 删除文件的接口（可以批量删除）
     * @param fileNames
     * @return
     */
    @DeleteMapping
    public Result deletePhotos(String [] fileNames){

        return fileUploadDelete.deletePhotos(fileNames);
    }

    /**
     * 上传帖子图片的接口
     * @param file
     * @return
     */
    @PostMapping("/PhotoForum")
    public String uploadForumPhoto(@NotNull MultipartFile file) {

        return fileUploadDelete.uploadPhoto(file,"ForumPhoto:");
    }

    /**
     * 上传商品图片的接口
     * @param file
     * @return
     */
    @PostMapping("/PhotoProduct")
    public String uploadProductPhoto(@NotNull MultipartFile file) {

        return fileUploadDelete.uploadPhoto(file,"ProductPhoto:");
    }

    /**
     * 上传用户头像的接口
     * @param file
     * @return
     */
    @PostMapping("/PhotoAvatar")
    public String uploadAvatarPhoto(@NotNull MultipartFile file) {

        String id = AccountHolder.getUser().getId();

        return fileUploadDelete.uploadPhoto(file,"avatar:"+id);
    }

}
