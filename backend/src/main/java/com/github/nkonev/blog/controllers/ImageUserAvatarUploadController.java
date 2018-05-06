package com.github.nkonev.blog.controllers;

import com.github.nkonev.blog.dto.UserAccountDetailsDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.sql.SQLException;
import java.util.UUID;

import static com.github.nkonev.blog.Constants.Urls.API;
import static com.github.nkonev.blog.Constants.Urls.IMAGE;

@RestController
public class ImageUserAvatarUploadController extends AbstractImageUploadController {

    public static final String POST_TEMPLATE = API+IMAGE+"/user/avatar";
    public static final String GET_TEMPLATE = POST_TEMPLATE + "/{id}.{ext}";

    public static final String imageType = "avatarImages";

    @PostMapping(POST_TEMPLATE)
    @PreAuthorize("isAuthenticated()")
    public ImageResponse postImage(
            @RequestPart(value = IMAGE_PART) MultipartFile imagePart,
            @NotNull @AuthenticationPrincipal UserAccountDetailsDTO userAccount
    ) throws SQLException {
        return super.postImage(
            "INSERT INTO images.user_avatar_image(img, content_type) VALUES (?, ?) RETURNING id",
            GET_TEMPLATE,
            imagePart
        );
    }

    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////

    @GetMapping(GET_TEMPLATE)
    public void getImage(
            @PathVariable("id")UUID id,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        super.getImage(
                "SELECT img, length(img) as content_length, content_type, create_date_time FROM images.user_avatar_image WHERE id = ?",
                id,
                request,
                response,
                imageType,
                "avatar image with id '" + id + "' not found"
        );
    }

    public int clearAvatarImages(){
        return jdbcTemplate.update("delete from images.user_avatar_image where id in (" +
                "select i.id from images.user_avatar_image i " +
                "left join auth.users u on u.avatar like '%' || '/api/image/user/avatar/' || i.id || '%' " +
                "where u.id is null" +
        ");");
    }
}
