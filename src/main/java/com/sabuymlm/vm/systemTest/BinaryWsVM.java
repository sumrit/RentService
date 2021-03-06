/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sabuymlm.vm.systemTest;
 
import com.sabuymlm.authen.SecurityUtil; 
import com.sabuymlm.model.systemTest.BinaryWsDefKey;
import com.sabuymlm.model.systemTest.BinaryWsDefine; 
import com.sabuymlm.model.systemTest.Position; 
import com.sabuymlm.service.SystemTestService; 
import com.sabuymlm.vm.CommonVM;  
import java.io.Serializable;
import java.util.Date; 
import org.springframework.transaction.annotation.Transactional; 
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.Init; 
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zkplus.spring.DelegatingVariableResolver; 
/**
 *
 * @author MY-TENG
 */
@VariableResolver(DelegatingVariableResolver.class)
public class BinaryWsVM extends AddCommonRefSponsorDefineVM<BinaryWsDefine,BinaryWsDefine> implements Serializable {

    @WireVariable
    private SystemTestService systemTestService;   
    
    @Init 
    public void init(@ContextParam(ContextType.VIEW) Component view
            , @ExecutionArgParam(CommonVM.PARAM_NAME_OBJECT) BinaryWsDefine item
            , @ExecutionArgParam("icon") String icon
            , @ExecutionArgParam("headerLabel") String headerLabel) {
        selectItems.addAll(systemTestService.findAllBinaryWsByCompany()) ;
        pageHasDetail = false ;
        super.initial(item, icon, headerLabel);      
        setStatusEdit();
    }

    @Override
    protected void setEditItem() { 
        selectItems.addAll(systemTestService.findAllBinaryWsByCompany()) ;
    }

    @Override
    protected void setNewItem() { 
        if(selectItems.isEmpty()) {
            setStatusAdd();
            positions.addAll(systemTestService.findAllPositions()); 
            selectItems.clear();
            for(Position pos : positions){
                BinaryWsDefine itm = new BinaryWsDefine(); 
                BinaryWsDefKey itemKey = new BinaryWsDefKey(); 
                itemKey.setCompany(SecurityUtil.getUserDetails().getCompany()); 
                itemKey.setPosition(pos); 
                itm.setId(itemKey);
                itm.setWsPcent(0.0f);
                itm.setStPcent(0.0f);
                itm.setMaxPv(0.0f);
                itm.setMaxComm(0.0f); 
                itm.setCircleUnit(1); 
                selectItems.add(itm);
            } 
        }else {
            setStatusEdit();
        }
    }   

    @Transactional
    @Override
    protected void saveItem() { 
        selectItems = systemTestService.saveBinaryWsDefine(selectItems);
    }  
    
    @Override
    protected void setItems(){ 
        for(BinaryWsDefine def: selectItems){
            if(positions.isEmpty()){
                def.setUpdateDate(new Date());
                def.setUpdateUser(SecurityUtil.getUserDetails().getUser());
            }else {
                def.setCreateDate(new Date());
                def.setCreateUser(SecurityUtil.getUserDetails().getUser());
            } 
        } 
    }   
    
    @Override
    protected void privateValidate() {  
    } 
    
    private void calculateItem(BinaryWsDefine item, int select){
        float wk = 0f, st = 0f , maxPv =0f , maxComm =0f;
        if(item.getWsPcent() != null ){
            wk = item.getWsPcent() ;
        }
        if(item.getStPcent() != null ){
            st = item.getStPcent() ;
        }
        if(item.getMaxPv() != null ){
            maxPv = item.getMaxPv();
        }
        if(item.getMaxComm() != null ){
            maxComm = item.getMaxComm();
        }
        if(select == 0 ){
            if(maxPv > 0 && (wk+st)>0) {
                item.setMaxComm( (maxPv*(wk+st))/100.0f );
            }else {
                item.setMaxComm(0f);
            }  
        }else {
            if(maxComm > 0 && (wk+st)>0) {
                item.setMaxPv( (maxComm*100.0f)/(wk+st) );
            }else {
                item.setMaxPv(0f);
            }  
        }
    }
    @Command(value = {"onCalculateMaxComm"})
    @NotifyChange({"item.maxComm"})
    public void calculateMaxCom(@BindingParam("item") BinaryWsDefine item ) { 
       calculateItem( item, 0) ;
    } 
    @Command(value = {"onCalculateMaxPv"})
    @NotifyChange({"item.maxPv"})
    public void calculateMaxPv(@BindingParam("item") BinaryWsDefine item ) { 
        calculateItem( item, 1) ;
    } 
     
    
}
