package com.vn.otech.billing.repository;

import com.vn.otech.billing.entity.BillingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingSettingRepository extends JpaRepository<BillingSetting, String> {
}
