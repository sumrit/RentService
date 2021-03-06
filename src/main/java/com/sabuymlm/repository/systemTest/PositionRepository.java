/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sabuymlm.repository.systemTest;
     
import com.sabuymlm.model.admin.Company; 
import com.sabuymlm.model.systemTest.Position; 
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.data.jpa.repository.Query;

/**
 *
 * @author bugteng
 */
public interface PositionRepository extends JpaRepository<Position, Integer> { 

    @Query("select u from Position u where ( UPPER(u.name) like UPPER(?1) OR UPPER(u.sponsorOrAndState) like UPPER(?1) ) "      
            + " and u.company = ?2 ")
    public Page<Position> findByLike(String keyword, Company company, Pageable pageRequest);
    
    @Query("select u from Position u where u.company = ?1 ")
    public List<Position> findByLike(Company company , Sort sort );  
     
    
}
