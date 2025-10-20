package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.ValidationResult;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.UserProfile;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CounterpartyRepository;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Service
public class TradeValidationService {

    private static final Logger log = LoggerFactory.getLogger(TradeValidationService.class);

    public static final String OPERATION_CREATE    = "CREATE";
    public static final String OPERATION_AMEND     = "AMEND";
    public static final String OPERATION_TERMINATE = "TERMINATE";
    public static final String OPERATION_CANCEL    = "CANCEL";
    public static final String OPERATION_VIEW      = "VIEW";

    @Autowired private ApplicationUserRepository applicationUserRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private CounterpartyRepository counterpartyRepository;

    @Transactional
    public ValidationResult validateTradeBusinessRules(TradeDTO tradeDTO) {
        ValidationResult result = ValidationResult.ok();
        applyDateRules(tradeDTO, result);
        applyCrossLegRules(tradeDTO.getTradeLegs(), tradeDTO, result);
        applyEntityStatusRules(tradeDTO, result);
        return result;
    }

    @Transactional
    public boolean validateUserPrivileges(String userId, String operation, TradeDTO tradeDTO) {
        Optional<ApplicationUser> userOpt = resolveUser(userId);
        if (userOpt.isEmpty()) {
            log.warn("Privilege check: user not found for id '{}'", userId);
            return false;
        }

        ApplicationUser user = userOpt.get();

    
        if (!user.isActive()) {
            log.warn("Privilege check: user '{}' is not active", user.getLoginId());
            return false;
        }

        String role = resolveUserRole(user);
        if (role == null) {
            log.warn("Privilege check: role not resolvable for user '{}'", user.getLoginId());
            return false; 
        }

        String op = (operation == null ? "" : operation).toUpperCase(Locale.ROOT);

        switch (role) {
            case "TRADER":
                return opEqualsAny(op, OPERATION_CREATE, OPERATION_AMEND, OPERATION_TERMINATE, OPERATION_CANCEL, OPERATION_VIEW);
            case "SALES":
                return opEqualsAny(op, OPERATION_CREATE, OPERATION_AMEND, OPERATION_VIEW);
            case "MIDDLE_OFFICE":
                return opEqualsAny(op, OPERATION_AMEND, OPERATION_VIEW);
            case "SUPPORT":
                return opEqualsAny(op, OPERATION_VIEW);
            default:
                log.warn("Privilege check: unknown role '{}' for user '{}'", role, user.getLoginId());
                return false;
        }
    }

    @Transactional
    public ValidationResult validateTradeLegConsistency(List<TradeLegDTO> legs) {
        ValidationResult result = ValidationResult.ok();
        applyCrossLegRules(legs, null, result);
        return result;
    }


    private void applyDateRules(TradeDTO tradeDTO, ValidationResult result) {
        LocalDate tradeDate    = tradeDTO.getTradeDate();
        LocalDate startDate    = tradeDTO.getTradeStartDate();
        LocalDate maturityDate = tradeDTO.getTradeMaturityDate();

        if (tradeDate != null && startDate != null && startDate.isBefore(tradeDate)) {
            result.addError("Start date cannot be before trade date.");
        }
        if (startDate != null && maturityDate != null && maturityDate.isBefore(startDate)) {
            result.addError("Maturity date cannot be before start date.");
        }
        if (tradeDate != null && maturityDate != null && maturityDate.isBefore(tradeDate)) {
            result.addError("Maturity date cannot be before trade date.");
        }
        if (tradeDate != null) {
            LocalDate cutoff = LocalDate.now().minusDays(30);
            if (tradeDate.isBefore(cutoff)) {
                result.addError("Trade date cannot be more than 30 days in the past.");
            }
        }
    }

    private void applyCrossLegRules(List<TradeLegDTO> legs, TradeDTO tradeDTO, ValidationResult result) {
        if (legs == null || legs.size() != 2) {
            return; 
        }

        TradeLegDTO leg1 = legs.get(0);
        TradeLegDTO leg2 = legs.get(1);

        LocalDate maturity = tradeDTO != null ? tradeDTO.getTradeMaturityDate() : null;
        if (maturity == null) {
            result.addError("Trade maturity date must be provided.");
        }

        String leg1PayRec = safeLower(leg1.getPayReceiveFlag());
        String leg2PayRec = safeLower(leg2.getPayReceiveFlag());
        if (leg1PayRec != null && leg2PayRec != null && leg1PayRec.equals(leg2PayRec)) {
            result.addError("Legs must have opposite pay/receive flags.");
        }

        validateLegTypeAndFields("leg1", leg1, result);
        validateLegTypeAndFields("leg2", leg2, result);
    }

    private void validateLegTypeAndFields(String label, TradeLegDTO leg, ValidationResult result) {
        String legType = safeLower(leg.getLegType()); 
        if ("floating".equals(legType)) {
            if (isBlank(leg.getIndexName())) {
                result.addError(label + ": floating leg must have an index specified.");
            }
        } else if ("fixed".equals(legType)) {
            if (leg.getRate() == null) {
                result.addError(label + ": fixed leg must have a valid rate.");
            }
        }
    }

    private void applyEntityStatusRules(TradeDTO tradeDTO, ValidationResult result) {
        if (tradeDTO.getBookId() != null) {
            Optional<Book> b = bookRepository.findById(tradeDTO.getBookId());
            if (b.isEmpty() || !b.get().isActive()) {
                result.addError("Book must exist and be active.");
            }
        } else if (!isBlank(tradeDTO.getBookName())) {
            bookRepository.findByBookName(tradeDTO.getBookName())
                    .filter(Book::isActive)
                    .orElseGet(() -> { result.addError("Book must exist and be active."); return null; });
        }

        if (tradeDTO.getCounterpartyId() != null) {
            Optional<Counterparty> c = counterpartyRepository.findById(tradeDTO.getCounterpartyId());
            if (c.isEmpty() || !c.get().isActive()) {
                result.addError("Counterparty must exist and be active.");
            }
        } else if (!isBlank(tradeDTO.getCounterpartyName())) {
            counterpartyRepository.findByName(tradeDTO.getCounterpartyName())
                    .filter(Counterparty::isActive)
                    .orElseGet(() -> { result.addError("Counterparty must exist and be active."); return null; });
        }

        if (tradeDTO.getTraderUserId() != null) {
            Optional<ApplicationUser> u = applicationUserRepository.findById(tradeDTO.getTraderUserId());
            if (u.isEmpty() || !u.get().isActive()) {
                result.addError("Trader user must exist and be active.");
            }
        } else if (!isBlank(tradeDTO.getTraderUserName())) {
            applicationUserRepository.findByLoginId(tradeDTO.getTraderUserName().toLowerCase(Locale.ROOT))
                    .filter(ApplicationUser::isActive)
                    .orElseGet(() -> { result.addError("Trader user must exist and be active."); return null; });
        }

        if (tradeDTO.getTradeInputterUserId() != null) {
            Optional<ApplicationUser> u = applicationUserRepository.findById(tradeDTO.getTradeInputterUserId());
            if (u.isEmpty() || !u.get().isActive()) {
                result.addError("Inputter user must exist and be active.");
            }
        } else if (!isBlank(tradeDTO.getInputterUserName())) {
            applicationUserRepository.findByLoginId(tradeDTO.getInputterUserName().toLowerCase(Locale.ROOT))
                    .filter(ApplicationUser::isActive)
                    .orElseGet(() -> { result.addError("Inputter user must exist and be active."); return null; });
        }
    }

   
    private Optional<ApplicationUser> resolveUser(String userId) {
        if (userId == null || userId.isBlank()) return Optional.empty();
        try {
            long id = Long.parseLong(userId);
            return applicationUserRepository.findById(id);
        } catch (NumberFormatException ex) {
            return applicationUserRepository.findByLoginId(userId.toLowerCase(Locale.ROOT));
        }
    }

    private String resolveUserRole(ApplicationUser user) {
        UserProfile profile = user.getUserProfile();
        if (profile == null || profile.getUserType() == null) return null;
        return profile.getUserType().toUpperCase(Locale.ROOT);
    }

    private static boolean opEqualsAny(String op, String... allowed) {
        for (String a : allowed) {
            if (op.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    private static String safeLower(String s) {
        return (s == null) ? null : s.toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}