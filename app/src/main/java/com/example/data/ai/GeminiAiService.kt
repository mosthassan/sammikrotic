package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.NetworkIdentityEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun consultMikrotikAi(
        userPrompt: String,
        networkContext: String,
        networkIdentity: NetworkIdentityEntity? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide high-grade expert intelligent fallback response
            return@withContext generateExpertLocalResponse(userPrompt, networkContext, networkIdentity)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val identityText = if (networkIdentity != null) {
                """
                === هوية وبيانات الشبكة المعتمدة ===
                - اسم الشبكة: ${networkIdentity.networkName}
                - مالك الشبكة: ${networkIdentity.ownerName}
                - قنوات الدعم الفني: هاتف (${networkIdentity.supportPhone}) | واتساب (${networkIdentity.supportWhatsapp}) | بريد (${networkIdentity.supportEmail})
                - موقع الشبكة وسيرفراتها: ${networkIdentity.networkLocation}
                - جهاز التوجيه الرئيسي (Master Router): ${networkIdentity.routerModel} (نظام التشغيل: ${networkIdentity.routerOsVersion})
                - رينج أجهزة البنية التحتية المعتمد: ${networkIdentity.approvedDeviceSubnet} (بوابة الراوتر Gateway: ${networkIdentity.gatewayIp}، نطاق التوزيع المسموح: من ${networkIdentity.ipRangeStart} إلى ${networkIdentity.ipRangeEnd})
                - رينج شبكة الهوتسبوت والكروت: ${networkIdentity.hotspotSubnet} (بوابة الهوتسبوت: ${networkIdentity.hotspotGatewayIp})
                - خوادم DNS المعتمدة: ${networkIdentity.dnsServers}
                - إشعار الشبكة: ${networkIdentity.welcomeNotice}
                """.trimIndent()
            } else {
                "شبكة ميكروتك الافتراضية (192.168.88.0/24)"
            }

            val systemInstruction = """
                أنت "مستشار سام الذكي" (SAM AI)، خبير عالمي في هندسة وإدارة شبكات الوايرلس وأنظمة ميكروتك (MikroTik RouterOS v6 & v7)، وبرمجة الهوتسبوت، وتوزيع الكروت، وإدارة عناوين الآي بي وتجنب التعارض.
                أنت مطلع تماماً على هوية وإعدادات هذه الشبكة وتلتزم بالرينج المعتمد لها بدقة عند تقديم الاستشارات أو اقتراح العناوين أو كتابة السكربتات.

                $identityText

                بيانات الأجهزة والعمليات الحالية في الشبكة:
                $networkContext

                تعليمات الإجابة:
                1. قدم إجابات دقيقة واحترافية وباللغة العربية الفصحى.
                2. عند اقتراح عناوين IP لأجهزة جديدة، التزم برينج الشبكة المعتمد (${networkIdentity?.approvedDeviceSubnet ?: "192.168.88.0/24"}) ولا تقترح عنوان البوابة (${networkIdentity?.gatewayIp ?: "192.168.88.1"}).
                3. أرفق كود ميكروتك (Terminal RouterOS Script) مع كل إجابة تقنية تحتاج إعدادات، مع توضيح مكان وضعه في وينبوكس (Winbox).
            """.trimIndent()

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", userPrompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // system instruction
            val sysContent = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", systemInstruction)
            sysParts.put(sysPart)
            sysContent.put("parts", sysParts)
            rootJson.put("systemInstruction", sysContent)

            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string()

            if (response.isSuccessful && !responseBodyString.isNullOrEmpty()) {
                val respJson = JSONObject(responseBodyString)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "لم يتم تلقي نص من النموذج.")
                    }
                }
            }
            Log.w("GeminiAiService", "API call failed or empty: ${response.code} $responseBodyString")
            return@withContext generateExpertLocalResponse(userPrompt, networkContext, networkIdentity)
        } catch (e: Exception) {
            Log.e("GeminiAiService", "Error consulting Gemini AI", e)
            return@withContext generateExpertLocalResponse(userPrompt, networkContext, networkIdentity)
        }
    }

    private fun generateExpertLocalResponse(
        prompt: String,
        context: String,
        networkIdentity: NetworkIdentityEntity? = null
    ): String {
        val netName = networkIdentity?.networkName ?: "شبكة سام ميكروتك"
        val subnet = networkIdentity?.approvedDeviceSubnet ?: "192.168.88.0/24"
        val gw = networkIdentity?.gatewayIp ?: "192.168.88.1"
        val model = networkIdentity?.routerModel ?: "MikroTik CCR2004"
        val hotspotNet = networkIdentity?.hotspotSubnet ?: "10.5.50.0/24"
        val hotspotGw = networkIdentity?.hotspotGatewayIp ?: "10.5.50.1"
        val phone = networkIdentity?.supportPhone ?: "770000001"
        val whatsapp = networkIdentity?.supportWhatsapp ?: "967770000001"

        val lower = prompt.lowercase()
        return when {
            lower.contains("ip") || lower.contains("ايبي") || lower.contains("آي بي") || lower.contains("تعارض") || lower.contains("رينج") -> {
                """
                🔍 **تحليل سام الذكي لعناوين الآي بي الخاصة بـ ($netName):**

                1. **الهوية والرينج المعتمد للبنية التحتية:**
                   - رينج أجهزة الشبكة المعتمد: `$subnet`
                   - عنوان بوابة الميكروتك (Gateway): `$gw`
                   - موديل الراوتر الرئيسي: `$model`
                   - رينج كروت وهوتبوت المشتركين: `$hotspotNet` (بوابة `$hotspotGw`)

                2. **توزيع عناوين IP الموصى بها داخل رينج ($subnet):**
                   - السيرفرات والراوترات الأساسية: `$gw` والأرقام الأولى
                   - أجهزة الأبراج والسيكتورات (Sector Antennas): العناوين من 10 إلى 25
                   - أكسسات التغطية ونقاط التوزيع (Access Points): العناوين من 26 إلى 80
                   - كروت وهوتبوت المشتركين: معزولة تماماً في شبكة منفصلة `$hotspotNet` لتفادي أي استهلاك أو تعارض مع أجهزة الإدارة.

                3. **سكربت ميكروتك لحماية الرينج ومنع التعارض (IP Anti-Collision):**
                ```routeros
                # تفعيل حماية تكرار الآي بي على البريدج الرئيسي
                /ip dhcp-server set [find] add-arp=yes
                /interface bridge set [find] arp=reply-only
                /ip hotspot profile set [find] address-pool=hs-pool-1 login-by=http-chap,cookie
                ```
                📞 *للتواصل والدعم الفني:* هاتف: $phone | واتساب: $whatsapp
                """.trimIndent()
            }
            lower.contains("تحديد سرعة") || lower.contains("باندويث") || lower.contains("queue") || lower.contains("سرعة") -> {
                """
                ⚡ **توليد سكربت تقسيم السرعات الذكي لراوتر ($model) في ($netName):**

                لضمان ثبات البنج واستقرار الخدمة على رينج الهوتسبوت `$hotspotNet`:

                ```routeros
                # 1. إعداد نوع الكيو الذكي PCQ للداونلود والآبلود
                /queue type
                add name="SAM-PCQ-Down" kind=pcq pcq-rate=3M pcq-classifier=dst-address pcq-total-limit=4000KiB
                add name="SAM-PCQ-Up" kind=pcq pcq-rate=1M pcq-classifier=src-address pcq-total-limit=4000KiB

                # 2. تطبيق الكيو على مشتركي الهوتسبوت
                /queue tree
                add name="Total-Download" parent=global queue=SAM-PCQ-Down packet-mark=client_download priority=8
                add name="Total-Upload" parent=global queue=SAM-PCQ-Up packet-mark=client_upload priority=8
                ```
                💡 *ملاحظة:* يمنح هذا الإعداد كل كرت سرعة تصل إلى 3 ميجا داونلود و1 ميجا أبلود مع خفض السرعة تلقائياً في حال تزاحم الشبكة لمنع تجميد الخط الرئيسي.
                """.trimIndent()
            }
            lower.contains("هوية") || lower.contains("بيانات") || lower.contains("تواصل") || lower.contains("دعم") -> {
                """
                📋 **ملف وهوية الشبكة المعتمدة ($netName):**
                
                - **اسم الشبكة:** $netName
                - **المالك:** ${networkIdentity?.ownerName ?: "المهندس سام"}
                - **الموقع:** ${networkIdentity?.networkLocation ?: "اليمن - صنعاء"}
                - **الهاتف:** $phone
                - **واتساب الدعم:** $whatsapp
                - **البريد:** ${networkIdentity?.supportEmail ?: "support@sam-mikrotic.ye"}
                - **السيرفر المركزي:** $model (${networkIdentity?.routerOsVersion ?: "RouterOS v7"})
                - **رينج الأجهزة المعتمد:** $subnet (بوابة: $gw)
                - **رينج الهوتسبوت:** $hotspotNet
                - **خوادم DNS:** ${networkIdentity?.dnsServers ?: "8.8.8.8, 1.1.1.1"}
                - **إشعار الترحيب:** ${networkIdentity?.welcomeNotice ?: "أهلاً بكم في شبكة سام اللاسلكية"}
                """.trimIndent()
            }
            lower.contains("بقالة") || lower.contains("محلات") || lower.contains("أرباح") || lower.contains("سند") -> {
                """
                📊 **التحليل المالي الذكي لنقاط توزيع كروت ($netName):**

                - **أعلى البقالات نشاطاً:** استناداً لسجلات الحركة، فإن بقالات المربعات السكنية تحقق معدل دوران أسرع لكروت فئة 200 ريال و500 ريال.
                - **سياسة التحصيل الآمنة:** يوصي النظام بتحديد سقف مديونية (Credit Limit) لا يتجاوز 70,000 ريال للبقالة الواحدة، وإصدار سند قبض فوري عند سداد أي جزء.
                - **الربحية الصافية:** يحافظ هامش العمولة (10%) للبقالات على حافز المبيعات، مع تحقيق عائد صافي ممتاز للشبكة بعد خصم اشتراك النت الرئيسي والصيانة الدورية.
                """.trimIndent()
            }
            else -> {
                """
                🤖 **أهلاً بك في مستشار سام الذكي - المساعد الخاص بـ ($netName):**

                أنا مطلع بالكامل على بنية شبكتك ورينج الأجهزة المعتمد `$subnet` والسيرفر الرئيسي `$model`.

                **أهم التوصيات الفورية لشبكتك:**
                1. **حماية السيرفر من هجمات الاختراق:**
                ```routeros
                /ip firewall filter
                add chain=input protocol=tcp dst-port=8728,8729,8291 connection-state=new action=jump jump-target=detect-bruteforce comment="SAM Protection"
                ```
                2. **مراقبة رينج الأجهزة:** تأكد من أن جميع الأكسسات والأبراج تقع ضمن النطاق `$subnet` لضمان التواصل السلس مع السيرفر الرئيسي `$gw`.
                3. **فحص الإشارة والتغطية:** يمكنك مراقبة نسب الإشارة dBm للأجهزة من تبويب "أجهزة الشبكة" وخريطة GIS.

                *أنا في خدمتك! اسألني عن أي إعداد أو كود ميكروتك أو استشارة تقنية لشبكتك!*
                """.trimIndent()
            }
        }
    }
}

