package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AuthorizationService {

    public boolean hasAnyRole(User user, Role... roles) {
        if (user == null || user.getRole() == null || !user.isEnabled()) {
            return false;
        }
        return Arrays.stream(roles).anyMatch(role -> role == user.getRole());
    }

    public void requireAdmin(User user) {
        requireAnyRole(user, "Admin access required", Role.ADMIN);
    }

    public void requireReportsAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Reports", Role.ADMIN, Role.MANAGER);
    }

    public void requireProductsAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Products", Role.ADMIN, Role.MANAGER);
    }

    public void requireImportGoodsAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Import Goods", Role.ADMIN, Role.MANAGER);
    }

    public void requireMasterDataAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Master Data", Role.ADMIN);
    }

    public void requireAuditLogAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Audit Log", Role.ADMIN);
    }

    public void requireStocktakeAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Stocktake", Role.ADMIN, Role.MANAGER);
    }

    public void requireAccountsAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Accounts", Role.ADMIN);
    }

    public void requireOrderHistoryAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Order History", Role.ADMIN, Role.MANAGER, Role.STAFF);
    }

    public void requireReturnsRefundsAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Returns / Refunds", Role.ADMIN, Role.MANAGER, Role.STAFF);
    }

    public void requireExpensesAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Expenses", Role.ADMIN, Role.MANAGER);
    }

    public void requirePromotionsAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Promotions", Role.ADMIN, Role.MANAGER);
    }

    public void requireCustomersAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Customers", Role.ADMIN, Role.MANAGER);
    }

    public void requireSalesAccess(User user) {
        requireAnyRole(user, "You are not allowed to access Sales", Role.ADMIN, Role.MANAGER, Role.STAFF);
    }

    public void requireSettingsAccess(User user) {
        if (user == null || !user.isEnabled()) {
            throw new AuthorizationException("You are not allowed to access Settings");
        }
    }

    public void requireSettingsEdit(User user) {
        requireSettingsAccess(user);
    }

    public void requireProductWrite(User user) {
        requireAnyRole(user, "You are not allowed to modify products", Role.ADMIN, Role.MANAGER);
    }

    public void requireProductDelete(User user) {
        requireAnyRole(user, "Only admins can delete products", Role.ADMIN);
    }

    public void requireExpenseWrite(User user) {
        requireAnyRole(user, "You are not allowed to modify expenses", Role.ADMIN, Role.MANAGER);
    }

    public void requirePromotionWrite(User user) {
        requireAnyRole(user, "You are not allowed to modify promotions", Role.ADMIN, Role.MANAGER);
    }

    public boolean canViewAllOrders(User user) {
        return hasAnyRole(user, Role.ADMIN, Role.MANAGER);
    }

    public boolean canManageOrder(User actor, Order order) {
        if (order == null) {
            return false;
        }
        if (hasAnyRole(actor, Role.ADMIN, Role.MANAGER)) {
            return true;
        }
        return actor != null
            && actor.isEnabled()
            && actor.getRole() == Role.STAFF
            && order.getUser() != null
            && order.getUser().getId() != null
            && order.getUser().getId().equals(actor.getId());
    }

    public void requireManageOrder(User actor, Order order) {
        if (!canManageOrder(actor, order)) {
            throw new AuthorizationException("You are not allowed to manage this order");
        }
    }

    public boolean canAccessReports(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canAccessProducts(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canAccessImportGoods(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canAccessMasterData(User user) { return hasAnyRole(user, Role.ADMIN); }
    public boolean canAccessAuditLog(User user) { return hasAnyRole(user, Role.ADMIN); }
    public boolean canAccessStocktake(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canAccessAccounts(User user) { return hasAnyRole(user, Role.ADMIN); }
    public boolean canAccessSales(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER, Role.STAFF); }
    public boolean canAccessOrderHistory(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER, Role.STAFF); }
    public boolean canAccessReturnsRefunds(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER, Role.STAFF); }
    public boolean canAccessExpenses(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canAccessPromotions(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canAccessCustomers(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canAccessSettings(User user) { return user != null && user.isEnabled(); }
    public boolean canEditSettings(User user) { return canAccessSettings(user); }
    public boolean canDeleteProducts(User user) { return hasAnyRole(user, Role.ADMIN); }
    public boolean canWriteProducts(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canWriteExpenses(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }
    public boolean canWritePromotions(User user) { return hasAnyRole(user, Role.ADMIN, Role.MANAGER); }

    private void requireAnyRole(User user, String message, Role... roles) {
        if (!hasAnyRole(user, roles)) {
            throw new AuthorizationException(message);
        }
    }
}
