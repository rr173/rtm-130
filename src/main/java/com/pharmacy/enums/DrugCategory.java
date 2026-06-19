package com.pharmacy.enums;

public enum DrugCategory {
    NORMAL("普通药品"),
    ANTIBIOTIC("抗生素"),
    NARCOTIC("麻醉药品"),
    PSYCHOTROPIC("精神药品"),
    CARDIOVASCULAR("心血管"),
    GASTROINTESTINAL("消化系统"),
    RESPIRATORY("呼吸系统"),
    EXTERNAL("外用药品"),
    VACCINE("疫苗"),
    BIOLOGICAL("生物制剂"),
    INSULIN("胰岛素");

    private final String description;

    DrugCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isControlled() {
        return this == NARCOTIC || this == PSYCHOTROPIC;
    }
}
