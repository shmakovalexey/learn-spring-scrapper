package learn.spring.fssp.scraper.core.dto;

import lombok.Getter;
import lombok.Setter;
import learn.spring.fssp.scraper.core.dao.Production;

import javax.persistence.Column;

@Getter @Setter
public class ResultProduction {
    @Column(name = "exe_production")
    private String exe;
    private String name;
    private String details;
    private String subject;
    private String department;
    private String bailiff;
    @Column(name = "ip_end")
    private String end;

    public static ResultProduction fromProduction(Production production){
        ResultProduction rp = new ResultProduction();
        rp.setExe(production.getExe());
        rp.setName(production.getName());
        rp.setDetails(production.getDetails());
        rp.setSubject(production.getSubject());
        rp.setDepartment(production.getDepartment());
        rp.setBailiff(production.getBailiff());
        rp.setEnd(production.getEnd());
        return rp;
    }
}
