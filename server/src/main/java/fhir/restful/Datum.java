package fhir.restful;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

@Entity

//patient_age_range_low, patient_age_range_high,
// patient_gender, patient_plan_id, equipment_code,
// no_auth_needed, info_link

public class Datum {
    @Id @GeneratedValue
    private Long id;
    private String patient_age_range_low;
    private String patient_age_range_high;
    private String patient_gender;
    private String patient_plan_id;
    private String equipment_code;
    private String no_auth_needed;
    private String info_link;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatient_age_range_low() {
        return patient_age_range_low;
    }

    public void setPatient_age_range_low(String patient_age_range_low) {
        this.patient_age_range_low = patient_age_range_low;
    }

    public String getPatient_age_range_high() {
        return patient_age_range_high;
    }

    public void setPatient_age_range_high(String patient_age_range_high) {
        this.patient_age_range_high = patient_age_range_high;
    }

    public String getPatient_gender() {
        return patient_gender;
    }

    public void setPatient_gender(String patient_gender) {
        this.patient_gender = patient_gender;
    }

    public String getPatient_plan_id() {
        return patient_plan_id;
    }

    public void setPatient_plan_id(String patient_plan_id) {
        this.patient_plan_id = patient_plan_id;
    }

    public String getEquipment_code() {
        return equipment_code;
    }

    public void setEquipment_code(String equipment_code) {
        this.equipment_code = equipment_code;
    }

    public String getNo_auth_needed() {
        return no_auth_needed;
    }

    public void setNo_auth_needed(String no_auth_needed) {
        this.no_auth_needed = no_auth_needed;
    }

    public String getInfo_link() {
        return info_link;
    }

    public void setInfo_link(String info_link) {
        this.info_link = info_link;
    }



    @Override
    public String toString(){
        return this.equipment_code;
    }


}
