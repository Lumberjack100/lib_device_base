package com.shmedo.lib.device.base.iot_cmd.parser

import com.shmedo.lib.device.base.iot_cmd.IOTCommandResult
import com.shmedo.lib.device.base.iot_cmd.enums.IOTCommandType
import com.shmedo.lib.device.base.iot_cmd.interfaces.IOTResultParser
import com.shmedo.lib.device.base.iot_cmd.model.CommonSettingCmdResult
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeAnthropomorphicMovementInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeBaseInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeBasicConfigParamParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeBrakePadControlInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeCurrentStateInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeExecutiveAgencyInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeGuideGrooveCalibrationInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeInclinometerInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeLockedRotorDetectionInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeLowEnergyModeInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeMeasuringHoleDepthInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeMeterWheelParamParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeMotionStateParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeMotorMotionAngleInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeMotorMotionDistanceInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeMotorPowerInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeStepperMotorInfoParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeVoltageConfigParser
import com.shmedo.lib.device.base.iot_cmd.parser.adme.AdmeWorkModeParser
import com.shmedo.lib.device.base.iot_cmd.utils.IOTStringUtil.extractCommandType
import java.util.EnumMap

/**
 * 创建者:   gonghe <br></br>
 * 创建时间:  2020/8/31 <br></br>
 * 描述：     TODO #gh#
 */
class IOTParseManager private constructor() {
    private val parserMap: MutableMap<IOTCommandType, IOTResultParser<*>> = EnumMap(IOTCommandType::class.java)

    init {
        registerParse()
    }

    fun <T> parse(result: String): IOTCommandResult<T> {
        baseValidate(result)
        val tempCmd = result.replace("&&", "")
        val commandResult = IOTCommandResult<T>()

        //失败的指令处理
        if (tempCmd.contains(IOTCommandResult.ERROR_FLAG)) {
            val settingCmdResult = CommonSettingCmdResultParser.instance.parse(result)
            commandResult.isSuccess = false
            commandResult.message = settingCmdResult.reason
            return commandResult
        }
        val cmdType = extractCommandType(tempCmd)
        val parser = parserMap[cmdType]
        if (parser == null) {
            commandResult.isSuccess = false
            commandResult.message = "未找到命令：" + cmdType.toString() + "的解析器"
            commandResult.commandType = cmdType
            return commandResult
        }
        parser.validate(tempCmd)
        val data = parser.parse(tempCmd) as T
        commandResult.isSuccess = true
        commandResult.commandType = cmdType
        commandResult.result = data
        return commandResult
    }

    /**
     * 解析设置类指令
     *
     * @param result
     * @return
     */
    fun parseSettingCmd(result: String): CommonSettingCmdResult? {
        if (result.isEmpty()) {
            return null
        }
        if (result.length < IOTCommandResult.RESULT_MIN_LENGTH) {
            return null
        }
        if (!result.startsWith(IOTCommandResult.COMMAND_HEADER)) {
            return null
        }
        CommonSettingCmdResultParser.instance.validate(result)
        return CommonSettingCmdResultParser.instance.parse(result)
    }

    /**
     * 这个方式只执行初级的格式校验，具体的逻辑校验由Validator接口和StringValidator接口执行
     *
     * @param result
     */
    private fun baseValidate(result: String) {
        require(result.isNotEmpty()) { "result为空或者null" }
        require(result.length >= IOTCommandResult.RESULT_MIN_LENGTH) { "result长度过短:$result" }
        require(result.startsWith(IOTCommandResult.COMMAND_HEADER)) { "result格式错误:$result" }
    }

    /**
     * 将本包下的Parse注册到parserMap中
     */
    private fun registerParse() {
        val clazzes = listOf(

            DataCenterStatusParser::class.java,
            DataCenterInfoParser::class.java,

            AdmeBaseInfoParser::class.java,
            AdmeBasicConfigParamParser::class.java,
            AdmeMeterWheelParamParser::class.java,
            AdmeInclinometerInfoParser::class.java,
            AdmeMotionStateParser::class.java,
            AdmeStepperMotorInfoParser::class.java,
            AdmeExecutiveAgencyInfoParser::class.java,
            AdmeMeasuringHoleDepthInfoParser::class.java,
            AdmeMotorMotionDistanceInfoParser::class.java,
            AdmeGuideGrooveCalibrationInfoParser::class.java,
            AdmeMotorMotionAngleInfoParser::class.java,
            AdmeCurrentStateInfoParser::class.java,
            AdmeWorkModeParser::class.java,
            AdmeLowEnergyModeInfoParser::class.java,
            AdmeLockedRotorDetectionInfoParser::class.java,
            AdmeVoltageConfigParser::class.java,
            AdmeAnthropomorphicMovementInfoParser::class.java,
            AdmeBrakePadControlInfoParser::class.java,
            AdmeMotorPowerInfoParser::class.java

        )
        registerWithClass(clazzes)
    }

    private fun registerWithClass(classes: List<Class<*>>) {
        try {
            classes.forEach {
                val resultParser = it.newInstance() as IOTResultParser<*>
                if (!parserMap.containsKey(resultParser.commandType())) {
                    parserMap[resultParser.commandType()] = resultParser
                }
            }
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        private val ourInstance = IOTParseManager()
        val instance: IOTParseManager
            get() {
                if (ourInstance.parserMap.isEmpty()) {
                    ourInstance.registerParse()
                }
                return ourInstance
            }
    }
}