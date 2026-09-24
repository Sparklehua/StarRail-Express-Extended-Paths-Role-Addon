package org.agmas.pathsrole;

public enum Paths {
    DESTRUCTION("The Destruction", "毁灭"),
    THE_HUNT("The Hunt", "巡猎"),
    ERUDITION("The Erudition", "智识"),
    ABUNDANCE("The Abundance", "丰饶"),
    NIHILITY("The Nihility", "虚无"),
    PRESERVATION("The Preservation", "存护"),
    HARMONY("The Harmony", "同谐"),
    ELATION("The Elation", "欢愉"),
    VORACITY("The Voracity", "贪饕"),
    BEAUTY("The Beauty", "纯美"),
    PROPAGATION("The Propagation", "繁育"),
    ENIGMATA("The Enigmata", "神秘"),
    EQUILIBRIUM("The Equilibrium", "均衡"),
    ORDER("The Order", "秩序"),
    TRAILBLAZE("The Trailblaze", "开拓"),
    REMEMBRANCE("The Remembrance", "记忆");

    private final String englishName;
    private final String chineseName;

    Paths(String englishName, String chineseName) {
        this.englishName = englishName;
        this.chineseName = chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getChineseName() {
        return chineseName;
    }
}