/*
 *  Copyright (C) 2020 Xiaomi Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ozhera.monitor.service.api;

import org.apache.ozhera.app.api.message.HeraAppInfoModifyMessage;
import org.apache.ozhera.monitor.dao.model.AppMonitor;
import org.apache.ozhera.monitor.result.Result;
import org.apache.ozhera.monitor.service.model.ProjectInfo;

import java.util.List;

/**
 * @author zhangxiaowei6
 */
public interface AppMonitorServiceExtension {

    Result getResourceUsageUrlForK8s(Integer appId, String appName);

    Result getResourceUsageUrl(Integer appId, String appName) ;

    Result grafanaInterfaceList();

    Result initAppsByUsername(String userName);

    List<ProjectInfo> getAppsByUserName(String username);

    Boolean checkCreateParam(AppMonitor appMonitor);

    Boolean checkAppModifyStrategySearchCondition(HeraAppInfoModifyMessage message);

    void changeAlarmServiceToZone(Integer pageSize,String appName);
}