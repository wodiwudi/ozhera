/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ozhera.log.manager.service.impl;

import com.google.gson.Gson;
import org.apache.ozhera.app.api.response.AppBaseInfo;
import org.apache.ozhera.log.api.enums.DeployWayEnum;
import org.apache.ozhera.log.api.enums.ProjectTypeEnum;
import org.apache.ozhera.log.api.model.bo.MiLogMoneTransfer;
import org.apache.ozhera.log.api.model.dto.MontorAppDTO;
import org.apache.ozhera.log.api.model.vo.MiLogMoneEnv;
import org.apache.ozhera.log.api.service.MilogOpenService;
import org.apache.ozhera.log.manager.dao.MilogLogTailDao;
import org.apache.ozhera.log.manager.common.exception.MilogManageException;
import org.apache.ozhera.log.manager.model.pojo.MilogLogTailDo;
import org.apache.ozhera.log.manager.common.validation.OpenSourceValid;
import com.xiaomi.youpin.docean.plugin.dubbo.anno.Service;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author wtt
 * @version 1.0
 * @description
 * @date 2021/12/8 14:54
 */
@Slf4j
@Service(interfaceClass = MilogOpenService.class, group = "$dubbo.group")
public class MilogOpenServiceImpl implements MilogOpenService {

    @Resource
    private MilogLogTailDao milogLogtailDao;

    @Resource
    private OpenSourceValid openSourceValid;

    @Resource
    private HeraAppServiceImpl heraAppService;

    @Resource
    private Gson gson;


    @Override
    public MontorAppDTO queryHaveAccessMilog(Long iamTreeId, String bingId, Integer platformType) {
        MontorAppDTO montorAppDTO = new MontorAppDTO();
        if (null == iamTreeId) {
            return montorAppDTO;
        }
        AppBaseInfo appBaseInfo = heraAppService.queryByIamTreeId(iamTreeId, bingId, platformType);
        if (null == appBaseInfo) {
            return montorAppDTO;
        }
        List<MilogLogTailDo> logTailDos = milogLogtailDao.getLogTailByMilogAppId(appBaseInfo.getId().longValue());
        if (CollectionUtils.isNotEmpty(logTailDos)) {
            montorAppDTO.setAppId(Long.valueOf(appBaseInfo.getBindId()));
            montorAppDTO.setAppName(appBaseInfo.getAppName());
            montorAppDTO.setSource(appBaseInfo.getPlatformName());
            montorAppDTO.setIsAccess(true);
        }
        return montorAppDTO;
    }

    @Override
    public Long querySpaceIdByIamTreeId(Long iamTreeId) {
        AppBaseInfo appBaseInfo = heraAppService.queryByIamTreeId(iamTreeId, Strings.EMPTY, null);
        if (null == appBaseInfo) {
            return null;
        }
        List<MilogLogTailDo> milogLogtailDos = milogLogtailDao.getLogTailByMilogAppId(appBaseInfo.getId().longValue());
        if (CollectionUtils.isEmpty(milogLogtailDos)) {
            return null;
        }
        List<Long> spaceIds = milogLogtailDos.stream().map(MilogLogTailDo::getSpaceId).distinct().collect(Collectors.toList());
        return spaceIds.get(spaceIds.size() - 1);
    }

    @Override
    public MiLogMoneTransfer ypMoneEnvTransfer(MiLogMoneEnv logMoneEnv) {
        String errors = openSourceValid.validMiLogMoneEnv(logMoneEnv);
        if (StringUtils.isNotBlank(errors)) {
            throw new MilogManageException(errors);
        }
        log.info("youpin mione transfer milie,data:{}", gson.toJson(logMoneEnv));
        MiLogMoneTransfer miLogMoneTransfer = new MiLogMoneTransfer();
        //1.Find apps
        handleMilogAppInfo(logMoneEnv, miLogMoneTransfer);
        //2.Modify tail
        handleMilogAppTail(logMoneEnv, miLogMoneTransfer);
        //3.Modify the source of the app - not required after migration
//        handleAppSource(logMoneEnv, miLogMoneTransfer);
        return miLogMoneTransfer;
    }

    private void handleMilogAppTail(MiLogMoneEnv logMoneEnv, MiLogMoneTransfer miLogMoneTransfer) {
        // Query the old tail
        List<MilogLogTailDo> milogLogtailDos = milogLogtailDao.queryByMilogAppAndEnv(miLogMoneTransfer.getMilogAppId(), logMoneEnv.getOldEnvId());
        if (CollectionUtils.isEmpty(milogLogtailDos)) {
            return;
        }
        for (MilogLogTailDo milogLogtailDo : milogLogtailDos) {
            milogLogtailDo.setAppName(logMoneEnv.getNewAppName());
            milogLogtailDo.setEnvId(logMoneEnv.getNewEnvId());
            milogLogtailDo.setEnvName(logMoneEnv.getNewEnvName());
            if (Objects.equals(1, logMoneEnv.getRollback())) {
                milogLogtailDo.setDeployWay(DeployWayEnum.MIONE.getCode());
            }
            milogLogtailDao.update(milogLogtailDo);
        }
        miLogMoneTransfer.setEnvId(logMoneEnv.getNewEnvId());
        miLogMoneTransfer.setEnvName(logMoneEnv.getNewEnvName());
        miLogMoneTransfer.setTailNames(milogLogtailDos
                .stream()
                .map(MilogLogTailDo::getTail)
                .collect(Collectors.toList()));
    }

    private void handleMilogAppInfo(MiLogMoneEnv logMoneEnv, MiLogMoneTransfer miLogMoneTransfer) {
        //1.Find apps based on old IDs
        AppBaseInfo appBaseInfo = heraAppService.queryByAppId(logMoneEnv.getNewAppId(), ProjectTypeEnum.MIONE_TYPE.getCode());
        if (null == appBaseInfo) {
            appBaseInfo = heraAppService.queryByAppId(logMoneEnv.getNewAppId(), 20);
            if (null == appBaseInfo) {
                throw new MilogManageException("The app does not exist");
            }
        }
        miLogMoneTransfer.setMilogAppId(appBaseInfo.getId().longValue());
        miLogMoneTransfer.setAppId(logMoneEnv.getNewAppId());
        miLogMoneTransfer.setAppName(logMoneEnv.getNewAppName());
    }
}
