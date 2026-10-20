package com.planner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/** application.yml 의 planner.* 설정 */
@ConfigurationProperties(prefix = "planner")
public class PlannerProperties {

    /** 인터페이스/작업지시의 SOURCE_CODE. 이 프로그램이 만든 데이터를 식별한다. */
    private String sourceCode = "XXPP";
    /** CREATED_BY / LAST_UPDATED_BY 및 apps_initialize 에 쓰는 FND_USER.USER_ID */
    private Long userId;
    /** 입고예정일·리드타임 수정값 저장 파일 (EBS 에는 쓰지 않음) */
    private String storePath = System.getProperty("user.home") + "/.ebs-planner/settings.json";
    private final Wip wip = new Wip();
    private final MassLoad massLoad = new MassLoad();
    private final Planning planning = new Planning();

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    /** 화면 상단에 띄울 안내 문구 (공개 데모 등). 비우면 표시 안 함 */
    private String banner;
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    public String getStorePath() { return storePath; }
    public void setStorePath(String storePath) { this.storePath = storePath; }
    public Wip getWip() { return wip; }
    public MassLoad getMassLoad() { return massLoad; }
    public Planning getPlanning() { return planning; }

    public static class Wip {
        /** 1=미해제(Unreleased), 3=해제(Released) */
        private int statusType = 1;
        /** 1=라우팅 기준(시작일만 전달), 3=수동(시작/완료일 모두 전달) */
        private int schedulingMethod = 1;
        /** 비우면 화면 입력값 → WIP_PARAMETERS.DEFAULT_DISCRETE_CLASS 순으로 사용 */
        private String classCode;
        private String startTime = "08:00";
        private String completionTime = "17:00";

        public int getStatusType() { return statusType; }
        public void setStatusType(int statusType) { this.statusType = statusType; }
        public int getSchedulingMethod() { return schedulingMethod; }
        public void setSchedulingMethod(int schedulingMethod) { this.schedulingMethod = schedulingMethod; }
        public String getClassCode() { return classCode; }
        public void setClassCode(String classCode) { this.classCode = classCode; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getCompletionTime() { return completionTime; }
        public void setCompletionTime(String completionTime) { this.completionTime = completionTime; }
    }

    public static class MassLoad {
        /** true 면 인터페이스 적재 후 WIP Mass Load(WICMLP) 동시요청을 바로 제출 */
        private boolean submit = false;
        private Long respId;
        /** WIP 애플리케이션 ID */
        private Long respApplId = 706L;
        /** WICMLP 파라미터 – 인스턴스의 동시 프로그램 정의에서 확인 후 조정 */
        private String validationLevel = "0";
        private String printReport = "2";

        public boolean isSubmit() { return submit; }
        public void setSubmit(boolean submit) { this.submit = submit; }
        public Long getRespId() { return respId; }
        public void setRespId(Long respId) { this.respId = respId; }
        public Long getRespApplId() { return respApplId; }
        public void setRespApplId(Long respApplId) { this.respApplId = respApplId; }
        public String getValidationLevel() { return validationLevel; }
        public void setValidationLevel(String validationLevel) { this.validationLevel = validationLevel; }
        public String getPrintReport() { return printReport; }
        public void setPrintReport(String printReport) { this.printReport = printReport; }
    }

    public static class Planning {
        /** 자원 교대(BOM_RESOURCE_SHIFTS)가 없을 때 쓰는 일 가동시간 */
        private double defaultHoursPerDay = 8;
        private int maxDays = 180;
        /** 자원 UOM → 시간 환산계수. 여기에 없는 UOM 의 자원은 부하 계산에서 제외 */
        private Map<String, Double> uomHours = new LinkedHashMap<>();

        public Planning() {
            uomHours.put("HR", 1.0);
            uomHours.put("MIN", 1.0 / 60);
            uomHours.put("SEC", 1.0 / 3600);
        }

        public Double hoursFactor(String uom) {
            if (uom == null) return null;
            for (Map.Entry<String, Double> e : uomHours.entrySet()) {
                if (e.getKey().equalsIgnoreCase(uom.trim())) return e.getValue();
            }
            return null;
        }

        public double getDefaultHoursPerDay() { return defaultHoursPerDay; }
        public void setDefaultHoursPerDay(double defaultHoursPerDay) { this.defaultHoursPerDay = defaultHoursPerDay; }
        public int getMaxDays() { return maxDays; }
        public void setMaxDays(int maxDays) { this.maxDays = maxDays; }
        public Map<String, Double> getUomHours() { return uomHours; }
        public void setUomHours(Map<String, Double> uomHours) { this.uomHours = uomHours; }
    }
}
