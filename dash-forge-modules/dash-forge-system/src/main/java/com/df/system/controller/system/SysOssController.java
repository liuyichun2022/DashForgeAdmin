package com.df.system.controller.system;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.df.common.core.domain.R;
import com.df.common.core.validate.QueryGroup;
import com.df.common.log.annotation.Log;
import com.df.common.log.enums.BusinessType;
import com.df.common.mybatis.core.page.PageQuery;
import com.df.common.mybatis.core.page.TableDataInfo;
import com.df.common.web.core.BaseController;
import com.df.system.domain.bo.SysOssBo;
import com.df.system.domain.vo.SysOssUploadVo;
import com.df.system.domain.vo.SysOssVo;
import com.df.system.service.ISysOssService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传 控制层
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/resource/oss")
public class SysOssController extends BaseController {

    private final ISysOssService ossService;

    /**
     * 查询OSS对象存储列表
     */
    @SaCheckPermission("system:oss:list")
    @GetMapping("/list")
    public TableDataInfo<SysOssVo> list(@Validated(QueryGroup.class) SysOssBo bo, PageQuery pageQuery) {
        return ossService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询OSS对象基于id串
     *
     * @param ossIds OSS对象ID串
     */
    @SaCheckPermission("system:oss:list")
    @GetMapping("/listByIds/{ossIds}")
    public R<List<SysOssVo>> listByIds(@NotEmpty(message = "主键不能为空")
                                       @PathVariable Long[] ossIds) {
        List<SysOssVo> list = ossService.listByIds(Arrays.asList(ossIds));
        return R.ok(list);
    }

    /**
     * 上传OSS对象存储
     *
     * @param file 文件
     */
    @SaCheckPermission("system:oss:upload")
    @Log(title = "OSS对象存储", businessType = BusinessType.INSERT)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysOssUploadVo> upload(@RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            return R.fail("上传文件不能为空");
        }
        SysOssVo oss = ossService.upload(file);
        SysOssUploadVo uploadVo = new SysOssUploadVo();
        uploadVo.setUrl(oss.getUrl());
        uploadVo.setFileName(oss.getOriginalName());
        uploadVo.setOssId(oss.getOssId().toString());
        return R.ok(uploadVo);
    }

    /**
     * 下载OSS对象
     *
     * @param ossId OSS对象ID
     */
    @SaCheckPermission("system:oss:download")
    @GetMapping("/download/{ossId}")
    public void download(@PathVariable Long ossId, HttpServletResponse response) throws IOException {
        ossService.download(ossId, response);
    }

    /**
     * 删除OSS对象存储
     *
     * @param ossIds OSS对象ID串
     */
    @SaCheckPermission("system:oss:remove")
    @Log(title = "OSS对象存储", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ossIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ossIds) {
        return toAjax(ossService.deleteWithValidByIds(List.of(ossIds), true));
    }

}
