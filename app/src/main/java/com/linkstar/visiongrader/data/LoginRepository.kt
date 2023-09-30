package com.linkstar.visiongrader.data

import android.util.Log
import androidx.datastore.dataStore
import com.linkstar.visiongrader.VisionGraderApplication
import com.linkstar.visiongrader.data.UserDataStore.ACCESS_TOKEN_KEY
import com.linkstar.visiongrader.data.UserDataStore.USER_ID_KEY
import com.linkstar.visiongrader.data.UserDataStore.clearUser
import com.linkstar.visiongrader.data.UserDataStore.dataStore
import com.linkstar.visiongrader.data.UserDataStore.storeUser
import com.linkstar.visiongrader.data.model.LoggedInUser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */


@OptIn(DelicateCoroutinesApi::class)
class LoginRepository() {


    // in-memory cache of the loggedInUser object
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null

        GlobalScope.launch {
            val store = VisionGraderApplication.context.dataStore.data.first()
            if (store[USER_ID_KEY].isNullOrEmpty()) {
                return@launch
            }

            Log.d("REPO", "user: " + store[USER_ID_KEY] + " " + store[ACCESS_TOKEN_KEY])
//            user = LoggedInUser(
//                store[USERNAME_KEY]!!,
//                store[PASSWORD_KEY]!!,
//                store[ACCESS_TOKEN_KEY]!!,
//                store[USER_ID_KEY]!!,
//                store[USER_NAME_KEY]!!,
//                store[SCHOOL_ID_KEY]!!,
//                store[SCHOOL_CODE_KEY]!!,
//                store[SCHOOL_NAME_KEY]!!,
//                store[SCHOOL_SHORT_NAME_KEY]!!
//            )
        }
    }

    suspend fun logout() {
        user = null

        clearUser()
    }

    suspend fun login(username: String, password: String): Result<LoggedInUser> {

//        val result = Api.login(username, password)
        val result = "{\n" +
                "    \"statusCode\":200,\n" +
                "    \"message\":\"OK\",\n" +
                "    \"bizCode\":\"0\",\n" +
                "    \"params\":null,\n" +
                "    \"guuid\":\"197614fea1914893b4880bef6d8b969a\",\n" +
                "    \"createTime\":1661309456000,\n" +
                "    \"createUser\":\"admin\",\n" +
                "    \"updateTime\":1661309605000,\n" +
                "    \"updateUser\":\"jinpeilong_xgy77\",\n" +
                "    \"guuidsList\":null,\n" +
                "    \"loginId\":\"jinpeilong_xgy77\",\n" +
                "    \"password\":\"89c41fbc07d9fcb2fca0218e95ba5042\",\n" +
                "    \"fullName\":\"jinpeilongxgy77\",\n" +
                "    \"phoneNum\":\"\",\n" +
                "    \"bindFlg\":null,\n" +
                "    \"tenantUuid\":\"e646863b82f711eb889e9c5c8e915b91\",\n" +
                "    \"delFlg\":\"0\",\n" +
                "    \"kemuShort\":null,\n" +
                "    \"validFlg\":false,\n" +
                "    \"initPwdFlg\":false,\n" +
                "    \"pwdVersion\":1,\n" +
                "    \"openId\":null,\n" +
                "    \"roleUuid\":\"ROLE_0004\",\n" +
                "    \"vipFlg\":\"0\",\n" +
                "    \"vipExpiretime\":null,\n" +
                "    \"frozeExpireTime\":null,\n" +
                "    \"lastPasswordTime\":1661309604000,\n" +
                "    \"accessToken\":\"abc668a4481467c8fe5daf0db6886bf4\",\n" +
                "    \"roleTypeUuid\":\"ROLE_TYPE_001\",\n" +
                "    \"teacherUserDto\":null,\n" +
                "    \"subjectUserDto\":null,\n" +
                "    \"operativeDto\":{\n" +
                "        \"statusCode\":0,\n" +
                "        \"message\":null,\n" +
                "        \"bizCode\":\"0\",\n" +
                "        \"params\":null,\n" +
                "        \"guuid\":\"41312ec1e55e498dbdee20a755fbd26e\",\n" +
                "        \"createTime\":null,\n" +
                "        \"createUser\":null,\n" +
                "        \"updateTime\":null,\n" +
                "        \"updateUser\":null,\n" +
                "        \"guuidsList\":null,\n" +
                "        \"userUuid\":\"197614fea1914893b4880bef6d8b969a\",\n" +
                "        \"agentUuid\":null,\n" +
                "        \"schUuid\":\"8fa0892523a9459583998891c7e59141\",\n" +
                "        \"adminFlg\":null,\n" +
                "        \"delFlg\":null,\n" +
                "        \"loginId\":\"jinpeilong_xgy77\",\n" +
                "        \"fullName\":\"jinpeilongxgy77\",\n" +
                "        \"passWord\":\"89c41fbc07d9fcb2fca0218e95ba5042\",\n" +
                "        \"phoneNum\":\"\",\n" +
                "        \"roleId\":\"ROLE_0004\",\n" +
                "        \"roleName\":null,\n" +
                "        \"agentId\":\"5e38c4a8544f4574908e31ca71c96177\",\n" +
                "        \"agentName\":null,\n" +
                "        \"userRoleUuid\":\"ac852653d0a1483b91acc8560437035d\",\n" +
                "        \"agentCompany\":null,\n" +
                "        \"initPwdFlg\":false,\n" +
                "        \"tenantUuid\":\"e646863b82f711eb889e9c5c8e915b91\",\n" +
                "        \"school\":{\n" +
                "            \"statusCode\":0,\n" +
                "            \"message\":null,\n" +
                "            \"bizCode\":\"0\",\n" +
                "            \"params\":null,\n" +
                "            \"guuid\":\"8fa0892523a9459583998891c7e59141\",  // 学校ID\n" +
                "            \"createTime\":1616548933000,\n" +
                "            \"createUser\":\"admin\",\n" +
                "            \"updateTime\":1686394263000,\n" +
                "            \"updateUser\":\"xg002\",\n" +
                "            \"guuidsList\":null,\n" +
                "            \"schName\":\"测试第一小学\",   // 学校名称\n" +
                "            \"schShortName\":\"测试一小\",\n" +
                "            \"schCode\":\"CS001\",\n" +
                "            \"provinceAreaUuid\":\"1450000\",\n" +
                "            \"cityAreaUuid\":\"2454150\",\n" +
                "            \"schAreaUuid\":\"3454150\",\n" +
                "            \"schAreaCode\":null,\n" +
                "            \"schAddr\":\"学校地址学校地址\",  \n" +
                "            \"schType\":\"1\",\n" +
                "            \"agentUuid\":\"5e38c4a8544f4574908e31ca71c96177\",\n" +
                "            \"tenantUuid\":\"e646863b82f711eb889e9c5c8e915b91\",\n" +
                "            \"loginId\":null,\n" +
                "            \"localFileServerIp\":\"http://test-oss-edu.fjoss.xstore.ctyun.cn\",\n" +
                "            \"outerFileServerIp\":\"http://test-oss-edu.fjoss.xstore.ctyun.cn\",\n" +
                "            \"delFlg\":\"0\",\n" +
                "            \"fourRateJson\":\"{\\\"极优秀\\\":\\\"90,100\\\",\\\"优秀\\\":\\\"80,90\\\",\\\"良好率\\\":\\\"70,80\\\",\\\"及格率\\\":\\\"60,100\\\",\\\"低分率\\\":\\\"0,30\\\"}\",\n" +
                "            \"dbShema\":null,\n" +
                "            \"rankLevelJson\":\"{\\\"极优秀\\\":\\\"90,100\\\",\\\"优秀\\\":\\\"80,90\\\",\\\"良好率\\\":\\\"70,80\\\",\\\"及格率\\\":\\\"60,100\\\"}\",\n" +
                "            \"studentAlocationJson\":\"{\\\"1\\\":\\\"在籍\\\",\\\"2\\\":\\\"借读\\\"}\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"bindStuList\":null,\n" +
                "    \"dbSchema\":\"cjn_csxx\",\n" +
                "    \"userWxInfo\":null,\n" +
                "    \"xqbAccessRight\":0,\n" +
                "    \"biUserId\":null,\n" +
                "    \"stuUuid\":null\n" +
                "}"

        Log.d("REPO", "" + result)

        if (result.isNullOrEmpty()) {
            return Result.Error(IOException("Error logging in"))
        }

        val json = JSONTokener(result).nextValue() as JSONObject

        val status = json.getInt("statusCode")
        if (status != 200) {
            return Result.Error(IOException("Error logging in"))
        }


        val data = json.getJSONObject("operativeDto")
        val school = data.getJSONObject("school")


        val user = LoggedInUser(
            username,
            password,
            json.getString("accessToken"),
            data.getString("userUuid"),
            data.getString("fullName"),
            data.getString("schUuid"),
            school.getString("schCode"),
            school.getString("schName"),
            school.getString("schShortName")
        )

        setLoggedInUser(user)

        return Result.Success(user)


//        if (result is Result.Success) {
//            setLoggedInUser(result.data)
//        }
//
//        return result
    }


    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore


        storeUser(loggedInUser)
    }


}
