/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sabuymlm.vm.event;
  
import com.sabuymlm.authen.SecurityUtil; 
import com.sabuymlm.model.event.RentEvent; 
import com.sabuymlm.model.event.RentEventDetail;
import com.sabuymlm.service.EventService;
import com.sabuymlm.service.RunFormatService;
import com.sabuymlm.utils.DateUtils;
import com.sabuymlm.utils.FileEntry;
import com.sabuymlm.utils.Validations;
import com.sabuymlm.vm.CommonAddVM;
import com.sabuymlm.vm.CommonVM;   
import java.io.IOException;
import java.io.InputStream; 
import java.io.Serializable;  
import java.util.ArrayList;
import java.util.Date;  
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.bind.BindContext; 
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zkplus.spring.DelegatingVariableResolver; 

/**
 *
 * @author MY-TENG
 */
@VariableResolver(DelegatingVariableResolver.class)
public class AddRentEventVM extends CommonAddVM<RentEvent> implements Serializable {

    @WireVariable
    private EventService eventService;
    @WireVariable
    private RunFormatService runService;
    private InputStream is = null;
    private String fileName; 
    private List<RentEventDetail> selectedItems = new ArrayList<RentEventDetail>(); 
    @WireVariable
    private Validator validator;
    private final Set<ConstraintViolation> constraintViolations = new HashSet<ConstraintViolation>();
    @Init
    public void init(@ContextParam(ContextType.VIEW) Component view, @ExecutionArgParam(CommonVM.PARAM_NAME_OBJECT) RentEvent item, @ExecutionArgParam("icon") String icon, @ExecutionArgParam("headerLabel") String headerLabel) {
        initial(item, icon, headerLabel);  
    }

    @Override
    protected void setEditItem() {
        item = eventService.findByRentEventId(item.getId());
        fileName = item.getAttachFile();
    }

    @Override
    protected void setNewItem() {
        item = new RentEvent();
        item.setCode(runService.rentEventCode());
        item.setRentDate(new Date()); 
        item.setDiscAmount(0.0f);
        item.setNetAmount(0.0f); 
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isVisibleFileUpload() {
        return (fileName != null);
    }

    @Command("onUpload")
    @NotifyChange({"item", "fileName", "visibleFileUpload"})
    public void upload(@ContextParam(ContextType.BIND_CONTEXT) BindContext ctx) throws IOException {
        UploadEvent event = (UploadEvent) ctx.getTriggerEvent();
        is = event.getMedia().getStreamData();
        fileName = event.getMedia().getName();
    }

    @Command("onClear")
    @NotifyChange({"fileName", "visibleFileUpload"})
    public void onClear() {
        is = null;
        fileName = null;
        item.setAttachFile(null);
    } 

    @Transactional
    @Override
    protected void saveItem() {
         
        if (is instanceof InputStream) {
            String newFileName = FileEntry.getNewName(fileName + SecurityUtil.getUserDetails().getUser().getCompanyId(), fileName);
            FileEntry.saveUploadFile(is, item.getAttachPath(), newFileName);
            item.setAttachFile(newFileName);
        }
        if (item.getId() == null) {
            item.setCode(runService.rentEventCode());
            item.setCreateDate(new Date());
            item.setCreateUser(SecurityUtil.getUserDetails().getUser());
        } else {
            item.setUpdateDate(new Date());
            item.setUpdateUser(SecurityUtil.getUserDetails().getUser());
        }
        if (item.getRentCloseDate() instanceof Date) {
            item.setRentCloseUser(SecurityUtil.getUserDetails().getUser());
        } else {
            item.setRentCloseUser(null);
        } 
        if(item.getReRentStatus()==null){
           item.setReRentStatus("false"); 
        }
        item.setCompany(SecurityUtil.getUserDetails().getCompany());
        eventService.saveRentEvent(item);
    }
    
    @Command("onAddDetail")
    @NotifyChange({"item"})
    public void onAddDetail() {
        item.addToDetails(new RentEventDetail());  
        if(item.getId() == null){
            if(item.getItemDetails().size() == 1 && item.getCustomer() != null ){
                item.getItemDetails().get(0).setCurrentPrice(item.getCustomer().getRentPerMonth()); 
                item.getItemDetails().get(0).setQty(item.getRentMonth()); 
                item.getItemDetails().get(0).setDescription(item.getCustomer().getDescription()); 
                item.getItemDetails().get(0).setServiceCode(item.getCustomer().getRefCode()); 
                onCalculate();
            }
        }
    }

    @Command("onRemoveDetail")
    @NotifyChange({"item","selectedItems"})
    public void onRemoveDetail() {
        if (selectedItems.isEmpty()) {
            throw new RuntimeException(" ไม่ได้เลือกรายการที่ต้องการลบ ");
        }
        item.getItemDetails().removeAll(selectedItems); 
        selectedItems.clear();
        BindUtils.postNotifyChange(null, null, AddRentEventVM.this, ".");
    } 

    public List<RentEventDetail> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<RentEventDetail> selectedItems) {
        this.selectedItems = selectedItems;
    } 
    
    @Command("onCalculate")
    @NotifyChange({"item"})
    public void onCalculate() { 
        float totalAmount = 0f; 
        float discAmount = item.getDiscAmount() ;
        for(RentEventDetail dt:item.getItemDetails()){
            if(dt.getQty() instanceof Integer && dt.getCurrentPrice() instanceof Float ){
                dt.setTotalAmount(dt.getQty()*dt.getCurrentPrice()); 
            }else {
                dt.setTotalAmount(0f); 
            }
            totalAmount += dt.getTotalAmount();
        } 
        item.setTotalAmount(totalAmount); 
        item.setNetAmount(  totalAmount - discAmount );   
    } 
    
    @Command("calculateExpDate")
    @NotifyChange({"item"})
    public void calculateExpDate() {
        if(item.getRentMonth() != null ){
            if(item.getRentDate() != null ) {
                item.setExpireDate(DateUtils.addMonth(item.getRentDate(), item.getRentMonth())); 
            }
        } 
    } 
    
    private Set<ConstraintViolation> validateAll() {  
        constraintViolations.clear(); 
        constraintViolations.addAll(validator.validate(item));
        for(RentEventDetail dt:item.getItemDetails()){
            constraintViolations.addAll(validator.validate(dt));
        }
        return constraintViolations;
    } 

    @Override
    protected boolean validate() {
        if (!validateAll().isEmpty()) {  
            Validations.showValidationError(constraintViolations); 
            return false;
        }else {
            return true;
        }
    }
    
}
