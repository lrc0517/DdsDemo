package com.aispeech.ddsdemo.util;

import java.util.List;

/**
 * Created by Lucien on 2018/6/30.
 */

public class BeanAlarm {

    /**
     * intent : 设置提醒
     * timeLocation : +
     * relativeTime : 00:05:00
     * skillId : 100001852
     * intentName : 设置提醒
     * duiWidget : text
     * nlu : {"input":"五分钟后提醒我","version":"2018.5.30.16:26:49","skillVersion":"15","skillId":"100001852","skill":"提醒","res":"5a5ffc357e74450001606b34","timestamp":1530321404,"semantics":{"request":{"confidence":1,"slotcount":3,"slots":[{"name":"intent","value":"设置提醒"},{"rawvalue":"后","rawpinyin":"hou","value":"+","name":"时间定位","pos":[3,3]},{"rawvalue":"五分钟","rawpinyin":"wu fen zhong","value":"00:05:00","name":"相对时间","pos":[0,2]}],"task":"提醒"}},"systime":9.260986328125}
     */

    private String intent;
    private String timeLocation;
    private String relativeTime;
    private String skillId;
    private String intentName;
    private String duiWidget;
    private NluBean nlu;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getTimeLocation() {
        return timeLocation;
    }

    public void setTimeLocation(String timeLocation) {
        this.timeLocation = timeLocation;
    }

    public String getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(String relativeTime) {
        this.relativeTime = relativeTime;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getIntentName() {
        return intentName;
    }

    public void setIntentName(String intentName) {
        this.intentName = intentName;
    }

    public String getDuiWidget() {
        return duiWidget;
    }

    public void setDuiWidget(String duiWidget) {
        this.duiWidget = duiWidget;
    }

    public NluBean getNlu() {
        return nlu;
    }

    public void setNlu(NluBean nlu) {
        this.nlu = nlu;
    }

    public static class NluBean {
        /**
         * input : 五分钟后提醒我
         * version : 2018.5.30.16:26:49
         * skillVersion : 15
         * skillId : 100001852
         * skill : 提醒
         * res : 5a5ffc357e74450001606b34
         * timestamp : 1530321404
         * semantics : {"request":{"confidence":1,"slotcount":3,"slots":[{"name":"intent","value":"设置提醒"},{"rawvalue":"后","rawpinyin":"hou","value":"+","name":"时间定位","pos":[3,3]},{"rawvalue":"五分钟","rawpinyin":"wu fen zhong","value":"00:05:00","name":"相对时间","pos":[0,2]}],"task":"提醒"}}
         * systime : 9.260986328125
         */

        private String input;
        private String version;
        private String skillVersion;
        private String skillId;
        private String skill;
        private String res;
        private int timestamp;
        private SemanticsBean semantics;
        private double systime;

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getSkillVersion() {
            return skillVersion;
        }

        public void setSkillVersion(String skillVersion) {
            this.skillVersion = skillVersion;
        }

        public String getSkillId() {
            return skillId;
        }

        public void setSkillId(String skillId) {
            this.skillId = skillId;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String getRes() {
            return res;
        }

        public void setRes(String res) {
            this.res = res;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public SemanticsBean getSemantics() {
            return semantics;
        }

        public void setSemantics(SemanticsBean semantics) {
            this.semantics = semantics;
        }

        public double getSystime() {
            return systime;
        }

        public void setSystime(double systime) {
            this.systime = systime;
        }

        public static class SemanticsBean {
            /**
             * request : {"confidence":1,"slotcount":3,"slots":[{"name":"intent","value":"设置提醒","rawvalue":"后","rawpinyin":"hou","pos":[3,3]},{"rawvalue":"后","rawpinyin":"hou","value":"+","name":"时间定位","pos":[3,3]},{"rawvalue":"五分钟","rawpinyin":"wu fen zhong","value":"00:05:00","name":"相对时间","pos":[0,2]}],"task":"提醒"}
             */

            private RequestBean request;

            public RequestBean getRequest() {
                return request;
            }

            public void setRequest(RequestBean request) {
                this.request = request;
            }

            public static class RequestBean {
                /**
                 * confidence : 1
                 * slotcount : 3
                 * slots : [{"name":"intent","value":"设置提醒"},{"rawvalue":"后","rawpinyin":"hou","value":"+","name":"时间定位","pos":[3,3]},{"rawvalue":"五分钟","rawpinyin":"wu fen zhong","value":"00:05:00","name":"相对时间","pos":[0,2]}]
                 * task : 提醒
                 */

                private int confidence;
                private int slotcount;
                private String task;
                private List<SlotsBean> slots;

                public int getConfidence() {
                    return confidence;
                }

                public void setConfidence(int confidence) {
                    this.confidence = confidence;
                }

                public int getSlotcount() {
                    return slotcount;
                }

                public void setSlotcount(int slotcount) {
                    this.slotcount = slotcount;
                }

                public String getTask() {
                    return task;
                }

                public void setTask(String task) {
                    this.task = task;
                }

                public List<SlotsBean> getSlots() {
                    return slots;
                }

                public void setSlots(List<SlotsBean> slots) {
                    this.slots = slots;
                }

                public static class SlotsBean {
                    /**
                     * name : intent
                     * value : 设置提醒
                     * rawvalue : 后
                     * rawpinyin : hou
                     * pos : [3,3]
                     */

                    private String name;
                    private String value;
                    private String rawvalue;
                    private String rawpinyin;
                    private List<Integer> pos;

                    public String getName() {
                        return name;
                    }

                    public void setName(String name) {
                        this.name = name;
                    }

                    public String getValue() {
                        return value;
                    }

                    public void setValue(String value) {
                        this.value = value;
                    }

                    public String getRawvalue() {
                        return rawvalue;
                    }

                    public void setRawvalue(String rawvalue) {
                        this.rawvalue = rawvalue;
                    }

                    public String getRawpinyin() {
                        return rawpinyin;
                    }

                    public void setRawpinyin(String rawpinyin) {
                        this.rawpinyin = rawpinyin;
                    }

                    public List<Integer> getPos() {
                        return pos;
                    }

                    public void setPos(List<Integer> pos) {
                        this.pos = pos;
                    }
                }
            }
        }
    }
}
