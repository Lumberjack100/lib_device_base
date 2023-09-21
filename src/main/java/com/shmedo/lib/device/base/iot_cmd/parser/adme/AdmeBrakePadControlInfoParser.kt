package com.shmedo.lib.device.base.iot_cmd.parser.adme

import com.shmedo.lib.device.base.iot_cmd.enums.IOTCommandType
import com.shmedo.lib.device.base.iot_cmd.interfaces.IOTResultParser

import com.shmedo.lib.device.base.iot_cmd.model.adme.AdmeBrakePadControlInfo

/**
 * 创建者:   gonghe <br></br>
 * 创建时间:  2023/2/1 <br></br>
 * 描述：     TODO
 */
class AdmeBrakePadControlInfoParser : IOTResultParser<AdmeBrakePadControlInfo?> {
    override fun parse(result: String): AdmeBrakePadControlInfo? {
        val info = AdmeBrakePadControlInfo()
        return try {
            val keyValues = result.split("&").toTypedArray()
            val keyValueMap = HashMap<String, String>()
            for (keyValue in keyValues) {
                val strs = keyValue.split("=").toTypedArray()
                if (strs.size < 2) {
                    keyValueMap[strs[0]] = ""
                } else {
                    keyValueMap[strs[0]] = strs[1]
                }
            }
            info.mode = keyValueMap.getOrDefault("mode", "NullKey")
            info
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    override fun validate(result: String) {}
    override fun commandType(): IOTCommandType {
        return IOTCommandType.ADME_MD_GET_BRAKE_PAD_CONTROL
    }
}