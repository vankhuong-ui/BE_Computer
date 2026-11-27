package com.example.MyProject.PRODUCT.service;

import com.example.MyProject.PRODUCT.dto.branch.BranchRequest;
import com.example.MyProject.PRODUCT.dto.branch.BranchResponse;
import com.example.MyProject.PRODUCT.entity.Branch;

import java.util.List;

public interface BranchService {
    List<BranchResponse> listPublicBranches(String city);
    List<BranchResponse> listAllAdminBranches();
    BranchResponse createBranch(BranchRequest req);
    BranchResponse updateBranch(Long id, BranchRequest req);
    void deleteBranch(Long id);

    Branch requireActiveBranch(Long branchId);
    Branch requireActiveBranchByCodeOrName(String codeOrName);
    List<Branch> findActiveBranchEntities();
    Long findFirstBranchId();
}
