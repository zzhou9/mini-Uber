package demo;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@Embeddable
@RequiredArgsConstructor
public class UnitInfo {

    private String unitVin;
    private String engineMake;
    private String customerName;
    private String unitNumber;

    @SuppressWarnings("unused")
    UnitInfo() {
        this.unitVin = "";
    }

    public UnitInfo(String vin) {
    }


    public String getUnitVin() {
        return unitVin;
    }

    public void setUnitVin(String unitVin) {
        this.unitVin = unitVin;
    }
}