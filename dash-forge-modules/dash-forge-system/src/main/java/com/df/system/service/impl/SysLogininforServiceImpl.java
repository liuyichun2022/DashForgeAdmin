package com.df.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.common.core.constant.Constants;
import com.df.common.core.utils.MapstructUtils;
import com.df.common.core.utils.ServletUtils;
import com.df.common.core.utils.StringUtils;
import com.df.common.core.utils.ip.AddressUtils;
import com.df.common.log.event.LogininforEvent;
import com.df.common.mybatis.core.page.PageQuery;
import com.df.common.mybatis.core.page.TableDataInfo;
import com.df.common.satoken.utils.LoginHelper;
import com.df.system.domain.SysClient;
import com.df.system.domain.bo.SysLogininforBo;
import com.df.system.domain.vo.SysLogininforVo;
import com.df.system.service.ISysLogininforService;
import com.df.system.domain.SysLogininfor;
import com.df.system.mapper.SysLogininforMapper;
import com.df.system.service.ISysClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 系统访问日志情况信息 服务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLogininforServiceImpl implements ISysLogininforService {

    private final SysLogininforMapper baseMapper;

    private final ISysClientService clientService;

    /**
     * 记录登录信息
     *
     * @param logininforEvent 登录事件
     */
    @Async
    @EventListener
    public void recordLogininfor(LogininforEvent logininforEvent) {
        HttpServletRequest request = logininforEvent.getRequest();
        final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        final String ip = ServletUtils.getClientIP(request);
        // 客户端信息
        String clientid = request.getHeader(LoginHelper.CLIENT_KEY);
        SysClient client = null;
        if (StringUtils.isNotBlank(clientid)) {
            client = clientService.queryByClientId(clientid);
        }

        String address = AddressUtils.getRealAddressByIP(ip);
        StringBuilder s = new StringBuilder();
        s.append(getBlock(ip));
        s.append(address);
        s.append(getBlock(logininforEvent.getUsername()));
        s.append(getBlock(logininforEvent.getStatus()));
        s.append(getBlock(logininforEvent.getMessage()));
        // 打印信息到日志
        log.info(s.toString(), logininforEvent.getArgs());
        // 获取客户端操作系统
        String os = userAgent.getOs().getName();
        // 获取客户端浏览器
        String browser = userAgent.getBrowser().getName();
        // 封装对象
        SysLogininforBo logininfor = new SysLogininforBo();
        logininfor.setTenantId(logininforEvent.getTenantId());
        logininfor.setUserName(logininforEvent.getUsername());
        if (ObjectUtil.isNotNull(client)) {
            logininfor.setClientKey(client.getClientKey());
            logininfor.setDeviceType(client.getDeviceType());
        }
        logininfor.setIpaddr(ip);
        logininfor.setLoginLocation(address);
        logininfor.setBrowser(browser);
        logininfor.setOs(os);
        logininfor.setMsg(logininforEvent.getMessage());
        // 日志状态
        if (StringUtils.equalsAny(logininforEvent.getStatus(), Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER)) {
            logininfor.setStatus(Constants.SUCCESS);
        } else if (Constants.LOGIN_FAIL.equals(logininforEvent.getStatus())) {
            logininfor.setStatus(Constants.FAIL);
        }
        // 插入数据
        insertLogininfor(logininfor);
    }

    private String getBlock(Object msg) {
        if (msg == null) {
            msg = "";
        }
        return "[" + msg.toString() + "]";
    }

    @Override
    public TableDataInfo<SysLogininforVo> selectPageLogininforList(SysLogininforBo logininfor, PageQuery pageQuery) {
        Map<String, Object> params = logininfor.getParams();
        LambdaQueryWrapper<SysLogininfor> lqw = new LambdaQueryWrapper<SysLogininfor>()
            .like(StringUtils.isNotBlank(logininfor.getIpaddr()), SysLogininfor::getIpaddr, logininfor.getIpaddr())
            .eq(StringUtils.isNotBlank(logininfor.getStatus()), SysLogininfor::getStatus, logininfor.getStatus())
            .like(StringUtils.isNotBlank(logininfor.getUserName()), SysLogininfor::getUserName, logininfor.getUserName())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysLogininfor::getLoginTime, params.get("beginTime"), params.get("endTime"));
        if (StringUtils.isBlank(pageQuery.getOrderByColumn())) {
            pageQuery.setOrderByColumn("info_id");
            pageQuery.setIsAsc("desc");
        }
        Page<SysLogininforVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 新增系统登录日志
     *
     * @param bo 访问日志对象
     */
    @Override
    public void insertLogininfor(SysLogininforBo bo) {
        SysLogininfor logininfor = MapstructUtils.convert(bo, SysLogininfor.class);
        logininfor.setLoginTime(new Date());
        baseMapper.insert(logininfor);
    }

    /**
     * 查询系统登录日志集合
     *
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLogininforVo> selectLogininforList(SysLogininforBo logininfor) {
        Map<String, Object> params = logininfor.getParams();
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysLogininfor>()
            .like(StringUtils.isNotBlank(logininfor.getIpaddr()), SysLogininfor::getIpaddr, logininfor.getIpaddr())
            .eq(StringUtils.isNotBlank(logininfor.getStatus()), SysLogininfor::getStatus, logininfor.getStatus())
            .like(StringUtils.isNotBlank(logininfor.getUserName()), SysLogininfor::getUserName, logininfor.getUserName())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysLogininfor::getLoginTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SysLogininfor::getInfoId));
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds) {
        return baseMapper.deleteBatchIds(Arrays.asList(infoIds));
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLogininfor() {
        baseMapper.delete(new LambdaQueryWrapper<>());
    }
}
