package com.apartment.community;

import com.google.android.material.card.MaterialCardView;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apartment.community.adapter.ListRow;
import com.apartment.community.adapter.SimpleListAdapter;
import com.apartment.community.api.ApiConfig;
import com.apartment.community.feature.operations.OperationsActivity;
import com.apartment.community.model.Announcement;
import com.apartment.community.model.CommunityDocument;
import com.apartment.community.model.CommunityEvent;
import com.apartment.community.model.CommunityNotification;
import com.apartment.community.model.CommunityRole;
import com.apartment.community.model.Complaint;
import com.apartment.community.model.EmergencyContact;
import com.apartment.community.model.Resident;
import com.apartment.community.model.ServiceRequest;
import com.apartment.community.model.User;
import com.apartment.community.model.Visitor;
import com.apartment.community.repository.PersistentRepository;
import com.apartment.community.security.AccountStore;
import com.apartment.community.ui.Motion;
import com.apartment.community.ui.UiFeedback;
import com.apartment.community.util.Validation;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Single-activity native app with persistent local storage.
 * Stitch-inspired UI: blue primary (#2563EB), teal accents, neutral backgrounds,
 * rounded cards, clear typography, consistent spacing.
 *
 * Data is persisted via PersistentRepository (SharedPreferences + JSON).
 * Backend integration point: ApiConfig + ApiModels for future Spring Boot + MySQL.
 */
public class MainActivity extends AppCompatActivity {
    private static final int NAV_HOME = 1;
    private static final int NAV_SECOND = 2;
    private static final int NAV_THIRD = 3;
    private static final int NAV_PROFILE = 4;
    private static final int REQUEST_OPERATIONS = 4102;

    private final List<Route> backStack = new ArrayList<>();
    private PersistentRepository repository;
    private FrameLayout screenRoot;
    private BottomNavigationView bottomNav;
    private boolean animateNextRender = true;
    private boolean animateCurrentRender = true;
    private boolean isSubmitting = false;
    private boolean splashCompleted = false;

    private static final class Route {
        final String name;
        final String id;
        Route(String name) { this(name, ""); }
        Route(String name, String id) {
            this.name = name;
            this.id = id == null ? "" : id;
        }
    }

    private static final class Field {
        final TextInputLayout layout;
        final TextInputEditText input;
        Field(TextInputLayout layout, TextInputEditText input) {
            this.layout = layout;
            this.input = input;
        }
        String value() { return input.getText() == null ? "" : input.getText().toString().trim(); }
        String rawValue() { return input.getText() == null ? "" : input.getText().toString(); }
        void error(String value) { layout.setError(value); }
        void clearError() { layout.setError(null); }
    }

    private static final class Tile {
        final String title;
        final String caption;
        final String destination;
        @DrawableRes final int icon;
        Tile(String title, String caption, String destination, @DrawableRes int icon) {
            this.title = title;
            this.caption = caption;
            this.destination = destination;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        screenRoot = findViewById(R.id.screenRoot);

        // Initialize persistent repository
        try {
            repository = PersistentRepository.get(this);
        } catch (Exception e) {
            // Fallback: try to get without context if already initialized
            if (PersistentRepository.isInitialized()) {
                repository = PersistentRepository.get();
            } else {
                // Last resort: initialize with app context and seed
                repository = PersistentRepository.get(getApplicationContext());
            }
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int rootLeft = screenRoot.getPaddingLeft();
        final int rootTop = screenRoot.getPaddingTop();
        final int rootRight = screenRoot.getPaddingRight();
        final int rootBottom = screenRoot.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(screenRoot, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            boolean imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(systemBars.bottom, imeVisible ? ime.bottom : 0);
            view.setPadding(rootLeft + systemBars.left, rootTop + systemBars.top,
                    rootRight + systemBars.right, rootBottom + bottomInset);
            if (bottomNav != null) {
                bottomNav.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(screenRoot);

        if (savedInstanceState != null && savedInstanceState.containsKey("current_route")) {
            splashCompleted = savedInstanceState.getBoolean("splash_completed", true);
            // The session role is intentionally NOT restored from the bundle: it comes from the
            // persisted session (account record), so a crafted savedInstanceState cannot
            // re-apply a role the signed-in account does not hold.
            backStack.add(new Route(savedInstanceState.getString("current_route", "LOGIN"),
                    savedInstanceState.getString("current_argument", "")));
            if (!isAuthRoute(current().name) && (!repository.isAuthenticated() || repository.getCurrentUser().role == null)) {
                backStack.clear();
                backStack.add(new Route("LOGIN"));
            }
        } else {
            User current = repository.getCurrentUser();
            if (repository.isAuthenticated() && current != null && current.role != null
                    && current.email != null && !current.email.isEmpty()) {
                backStack.add(new Route("HOME"));
            } else {
                backStack.add(new Route("LOGIN"));
            }
        }
        render();
        if (!splashCompleted) {
            splashCompleted = true;
            showStartupSplashOverlay();
        }
    }

    private void showStartupSplashOverlay() {
        if (screenRoot == null || !Motion.areEnabled(this)) return;
        final View splashView = LayoutInflater.from(this).inflate(R.layout.view_splash, screenRoot, false);
        View logo = splashView.findViewById(R.id.splashLogo);
        View title = splashView.findViewById(R.id.splashTitle);
        View tagline = splashView.findViewById(R.id.splashTagline);
        View accentBar = splashView.findViewById(R.id.splashAccentBar);
        screenRoot.addView(splashView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Motion.animateSplash(splashView, logo, title, tagline, accentBar, () -> {
            if (screenRoot != null && splashView.getParent() == screenRoot) {
                screenRoot.removeView(splashView);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("splash_completed", splashCompleted);
        if (!backStack.isEmpty()) {
            Route route = current();
            outState.putString("current_route", route.name);
            outState.putString("current_argument", route.id);
        }
    }

    @Override
    public void onBackPressed() {
        if (isSubmitting) return;
        if (backStack.size() > 1) {
            backStack.remove(backStack.size() - 1);
            if (!isAuthRoute(current().name) && (!repository.isAuthenticated() || repository.getCurrentUser().role == null)) {
                rootRoute("LOGIN");
            } else {
                render();
            }
        } else if (!backStack.isEmpty()) {
            String currentRoute = current().name;
            if (isAuthRoute(currentRoute)) {
                if ("ROLE_PICKER".equals(currentRoute)) {
                    repository.logout();
                    rootRoute("LOGIN");
                } else {
                    super.onBackPressed();
                }
            } else if (!"HOME".equals(currentRoute) && repository.isAuthenticated()
                    && repository.getCurrentUser().role != null) {
                rootRoute("HOME");
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    private Route current() { return backStack.get(backStack.size() - 1); }

    private void navigate(String route, String id) {
        backStack.add(new Route(route, id));
        render();
    }

    private void navigate(String route) { navigate(route, ""); }

    private void rootRoute(String route) {
        backStack.clear();
        backStack.add(new Route(route));
        render();
    }

    private void replaceCurrent(String route, String id) {
        if (backStack.isEmpty()) backStack.add(new Route(route, id));
        else backStack.set(backStack.size() - 1, new Route(route, id));
        renderQuietly();
    }

    private boolean isAuthRoute(String route) {
        return "LOGIN".equals(route) || "REGISTER".equals(route) || "FORGOT".equals(route)
                || "RECOVER".equals(route) || "ROLE_PICKER".equals(route);
    }

    private boolean isTabRoute(String route) {
        return "HOME".equals(route) || "COMMUNITY".equals(route) || "WORK".equals(route)
                || "PROFILE".equals(route);
    }

    private void render() {
        if (screenRoot == null || backStack.isEmpty()) return;
        animateCurrentRender = animateNextRender;
        animateNextRender = true;
        isSubmitting = false;
        screenRoot.removeAllViews();
        bottomNav = null;
        Route route = current();
        if (isAuthRoute(route.name)) {
            if ("ROLE_PICKER".equals(route.name) && !repository.isAuthenticated()) {
                rootRoute("LOGIN");
                return;
            }
            renderAuth(route);
            return;
        }
        if (!repository.isAuthenticated() || repository.getCurrentUser().role == null) {
            rootRoute(repository.isAuthenticated() ? "ROLE_PICKER" : "LOGIN");
            return;
        }
        renderMain(route);
    }

    private void renderQuietly() {
        animateNextRender = false;
        render();
    }

    private void renderAuth(Route route) {
        if ("LOGIN".equals(route.name)) showLogin();
        else if ("REGISTER".equals(route.name)) showRegistration();
        else if ("FORGOT".equals(route.name)) showForgotPassword();
        else if ("RECOVER".equals(route.name)) showRecovery(route.id);
        else showRolePicker();
    }

    private void renderMain(Route route) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(color(R.color.background));
        shell.addView(createTopBar(route), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout panel = new FrameLayout(this);
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        shell.addView(panel, panelParams);
        renderContent(route, panel);

        BottomNavigationView bottom = createBottomNavigation(route);
        bottomNav = bottom;
        shell.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screenRoot.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (animateCurrentRender) Motion.enter(panel, 0L);
    }

    private void renderContent(Route route, FrameLayout panel) {
        switch (route.name) {
            case "HOME": showHome(panel); break;
            case "COMMUNITY": showCommunityHub(panel); break;
            case "WORK": showWorkHub(panel); break;
            case "PROFILE": showProfile(panel); break;
            case "SETTINGS": showSettings(panel); break;
            case "ANNOUNCEMENTS": case "RESIDENTS": case "COMPLAINTS": case "REQUESTS":
            case "VISITORS": case "EVENTS": case "EMERGENCY": case "NOTIFICATIONS":
            case "DOCUMENTS": showList(route, panel); break;
            case "DETAIL_ANNOUNCEMENT": case "DETAIL_RESIDENT": case "DETAIL_COMPLAINT":
            case "DETAIL_REQUEST": case "DETAIL_VISITOR": case "DETAIL_EVENT": case "DETAIL_EMERGENCY":
            case "DETAIL_NOTIFICATION": case "DETAIL_DOCUMENT": showDetail(route, panel); break;
            case "FORM_COMPLAINT": showComplaintForm(panel); break;
            case "FORM_REQUEST": showRequestForm(panel); break;
            case "FORM_VISITOR": showVisitorForm(panel); break;
            case "FORM_ANNOUNCEMENT": showAnnouncementForm(panel); break;
            case "FORM_EVENT": showEventForm(panel); break;
            case "FORM_RESIDENT": showResidentForm(panel); break;
            case "EDIT_PROFILE": showProfileForm(panel); break;
            default: showHome(panel); break;
        }
    }

    private View createTopBar(Route route) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(12), dp(16), dp(12));
        bar.setBackgroundColor(color(R.color.surface));
        // Add subtle bottom border
        bar.setElevation(dp(1));

        boolean tab = isTabRoute(route.name);
        if (!tab) {
            ImageButton back = iconButton(R.drawable.ic_back, "Go back");
            back.setOnClickListener(v -> onBackPressed());
            LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            backParams.setMargins(0, 0, dp(8), 0);
            bar.addView(back, backParams);
        } else if ("HOME".equals(route.name)) {
            ImageView brandLogo = new ImageView(this);
            brandLogo.setImageResource(R.drawable.ic_app_logo);
            brandLogo.setContentDescription(getString(R.string.app_name));
            LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(36), dp(36));
            logoParams.setMargins(0, 0, dp(10), 0);
            bar.addView(brandLogo, logoParams);
        }

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(heading, headingParams);

        TextView title = text(routeTitle(route), 19, R.color.text_primary, true);
        title.setMaxLines(1);
        heading.addView(title);
        String subtitle;
        if ("HOME".equals(route.name)) {
            subtitle = getString(R.string.community_name).toUpperCase(Locale.getDefault());
        } else if (tab) {
            CommunityRole role = repository.getCurrentUser().role;
            subtitle = role.getLabel() + " · " + safe(repository.getCurrentUser().apartmentUnit, "Community");
        } else {
            subtitle = getString(R.string.community_name);
        }
        TextView sub = text(subtitle, 11, R.color.text_muted, true);
        sub.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(2);
        heading.addView(sub, subParams);

        if ("HOME".equals(route.name)) {
            ImageButton notifications = iconButton(R.drawable.ic_bell,
                    "Notifications, " + repository.countUnreadNotifications() + " unread");
            notifications.setOnClickListener(v -> navigate("NOTIFICATIONS"));
            FrameLayout holder = new FrameLayout(this);
            holder.addView(notifications, new FrameLayout.LayoutParams(dp(44), dp(44)));
            if (repository.countUnreadNotifications() > 0) {
                View dot = new View(this);
                dot.setBackground(shape(color(R.color.danger), dp(20), color(R.color.surface), dp(2)));
                FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(9), dp(9), Gravity.TOP | Gravity.END);
                dotParams.setMargins(0, dp(2), dp(2), 0);
                holder.addView(dot, dotParams);
            }
            bar.addView(holder, new LinearLayout.LayoutParams(dp(44), dp(44)));
        } else if (canAdd(route.name)) {
            MaterialButton add = smallButton("Add", true);
            add.setIconResource(R.drawable.ic_add);
            add.setIconTint(ColorStateList.valueOf(Color.WHITE));
            add.setContentDescription("Add new item");
            add.setOnClickListener(v -> onAdd(route.name));
            bar.addView(add);
        } else if (tab) {
            ImageButton notifications = iconButton(R.drawable.ic_bell, "Open notifications");
            notifications.setOnClickListener(v -> navigate("NOTIFICATIONS"));
            bar.addView(notifications, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        return bar;
    }

    private String routeTitle(Route route) {
        switch (route.name) {
            case "HOME": return "Cedar Grove";
            case "COMMUNITY": return "Community";
            case "WORK": return repository.getCurrentUser().role == CommunityRole.SECURITY ? "Gate desk" : "Services";
            case "PROFILE": return "My profile";
            case "SETTINGS": return "Settings";
            case "RECOVER": return "Account recovery";
            case "ANNOUNCEMENTS": return "Announcements";
            case "RESIDENTS": return "Resident directory";
            case "COMPLAINTS": return repository.getCurrentUser().role == CommunityRole.MAINTENANCE ? "Work queue" : "Complaints";
            case "REQUESTS": return "Service requests";
            case "VISITORS": return "Visitor management";
            case "EVENTS": return "Community events";
            case "EMERGENCY": return "Emergency contacts";
            case "NOTIFICATIONS": return "Notifications";
            case "DOCUMENTS": return "Community documents";
            case "FORM_COMPLAINT": return "New complaint";
            case "FORM_REQUEST": return "New service request";
            case "FORM_VISITOR": return "Register visitor";
            case "FORM_ANNOUNCEMENT": return "New announcement";
            case "FORM_EVENT": return "Create event";
            case "FORM_RESIDENT": return "Add resident";
            case "EDIT_PROFILE": return "Edit profile";
            case "DETAIL_ANNOUNCEMENT": return "Announcement";
            case "DETAIL_RESIDENT": return "Resident details";
            case "DETAIL_COMPLAINT": return "Complaint details";
            case "DETAIL_REQUEST": return "Request details";
            case "DETAIL_VISITOR": return "Visitor details";
            case "DETAIL_EVENT": return "Event details";
            case "DETAIL_EMERGENCY": return "Contact details";
            case "DETAIL_NOTIFICATION": return "Notification";
            case "DETAIL_DOCUMENT": return "Document information";
            default: return getString(R.string.app_name);
        }
    }

    private BottomNavigationView createBottomNavigation(Route route) {
        BottomNavigationView nav = new BottomNavigationView(this);
        nav.setBackgroundColor(color(R.color.surface));
        nav.setElevation(dp(8));
        nav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        nav.setItemHorizontalTranslationEnabled(false);
        nav.setPadding(0, dp(4), 0, dp(4));
        CommunityRole role = repository.getCurrentUser().role;
        if (role == CommunityRole.SECURITY) {
            addNavItem(nav, NAV_HOME, 0, "Home", R.drawable.ic_home);
            addNavItem(nav, NAV_SECOND, 1, "Visitors", R.drawable.ic_visitors);
            addNavItem(nav, NAV_THIRD, 2, "Contacts", R.drawable.ic_phone);
        } else if (role == CommunityRole.MAINTENANCE) {
            addNavItem(nav, NAV_HOME, 0, "Home", R.drawable.ic_home);
            addNavItem(nav, NAV_SECOND, 1, "Complaints", R.drawable.ic_build);
            addNavItem(nav, NAV_THIRD, 2, "Requests", R.drawable.ic_calendar);
        } else {
            addNavItem(nav, NAV_HOME, 0, "Home", R.drawable.ic_home);
            addNavItem(nav, NAV_SECOND, 1, "Community", R.drawable.ic_people);
            addNavItem(nav, NAV_THIRD, 2, "Services", R.drawable.ic_build);
        }
        addNavItem(nav, NAV_PROFILE, 3, "Profile", R.drawable.ic_profile);
        nav.setSelectedItemId(selectedNavigationId(route));
        nav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case NAV_HOME: rootRoute("HOME"); return true;
                case NAV_SECOND:
                    if (role == CommunityRole.SECURITY) rootRoute("VISITORS");
                    else if (role == CommunityRole.MAINTENANCE) rootRoute("COMPLAINTS");
                    else rootRoute("COMMUNITY");
                    return true;
                case NAV_THIRD:
                    if (role == CommunityRole.SECURITY) rootRoute("EMERGENCY");
                    else if (role == CommunityRole.MAINTENANCE) rootRoute("REQUESTS");
                    else rootRoute("WORK");
                    return true;
                case NAV_PROFILE: rootRoute("PROFILE"); return true;
                default: return false;
            }
        });
        return nav;
    }

    private void addNavItem(BottomNavigationView nav, int id, int order, String label, @DrawableRes int icon) {
        android.view.MenuItem item = nav.getMenu().add(0, id, order, label);
        item.setIcon(icon);
        item.setCheckable(true);
    }

    private int selectedNavigationId(Route route) {
        String name = route.name;
        if ("HOME".equals(name)) return NAV_HOME;
        if ("PROFILE".equals(name) || "SETTINGS".equals(name) || "EDIT_PROFILE".equals(name)) return NAV_PROFILE;
        CommunityRole role = repository.getCurrentUser().role;
        if (role == CommunityRole.SECURITY) {
            if ("EMERGENCY".equals(name) || "DETAIL_EMERGENCY".equals(name)) return NAV_THIRD;
            if ("VISITORS".equals(name) || "DETAIL_VISITOR".equals(name) || "FORM_VISITOR".equals(name)) return NAV_SECOND;
            return NAV_HOME;
        }
        if (role == CommunityRole.MAINTENANCE) {
            if ("REQUESTS".equals(name) || "DETAIL_REQUEST".equals(name)) return NAV_THIRD;
            if ("COMPLAINTS".equals(name) || "DETAIL_COMPLAINT".equals(name)) return NAV_SECOND;
            return NAV_HOME;
        }
        if ("COMMUNITY".equals(name) || "RESIDENTS".equals(name) || "ANNOUNCEMENTS".equals(name)
                || "EVENTS".equals(name) || "DOCUMENTS".equals(name) || "NOTIFICATIONS".equals(name)
                || "EMERGENCY".equals(name) || "DETAIL_EMERGENCY".equals(name)
                || "DETAIL_NOTIFICATION".equals(name) || "FORM_ANNOUNCEMENT".equals(name)
                || "FORM_EVENT".equals(name) || "FORM_RESIDENT".equals(name)
                || name.startsWith("DETAIL_ANNOUNCEMENT") || name.startsWith("DETAIL_RESIDENT")
                || name.startsWith("DETAIL_EVENT") || name.startsWith("DETAIL_DOCUMENT")) return NAV_SECOND;
        return NAV_THIRD;
    }

    // --- AUTH SCREENS ---

    private void showLogin() {
        LinearLayout page = authPage(false);
        addBranding(page);
        TextView title = text(getString(R.string.welcome_title), 30, R.color.text_primary, true);
        addTopMargin(page, title, 20);
        page.addView(text(getString(R.string.app_tagline), 15, R.color.text_secondary, false));
        addTopMargin(page, text("SIGN IN", 11, R.color.primary, true), 28);
        TextView note = text("Enter your community account credentials. Data is stored locally on this device.", 13, R.color.text_secondary, false);
        note.setLineSpacing(dp(3), 1f);
        addTopMargin(page, note, 8);

        Field email = createField("Email address", "name@example.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
        addTopMargin(page, email.layout, 22);
        Field password = createField("Password", "At least 6 characters", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, false);
        password.layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        addTopMargin(page, password.layout, 12);

        // Clear errors on typing
        setupClearErrorOnType(email);
        setupClearErrorOnType(password);

        MaterialButton login = fullButton("Sign in", true);
        addTopMargin(page, login, 18);
        MaterialButton demo = fullButton("Create account", false);
        addTopMargin(page, demo, 10);
        demo.setOnClickListener(v -> {
            if (!isSubmitting) navigate("REGISTER");
        });

        LinearLayout links = new LinearLayout(this);
        links.setGravity(Gravity.CENTER);
        links.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton forgot = textButton("Forgot password?");
        links.addView(forgot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        addTopMargin(page, links, 10);
        forgot.setOnClickListener(v -> {
            if (!isSubmitting) navigate("FORGOT");
        });

        login.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean ok = true;
            Validation.Result emailRes = Validation.validateEmail(email.value());
            if (!emailRes.valid) { email.error(emailRes.error); ok = false; } else email.clearError();
            Validation.Result passRes = Validation.validatePassword(password.rawValue());
            if (!passRes.valid) { password.error(passRes.error); ok = false; } else password.clearError();

            if (!ok) {
                Motion.shakeError(login);
                return;
            }

            isSubmitting = true;
            Motion.setButtonLoading(login, "Signing in...", email.input, password.input, demo, forgot);

            final String attemptedEmail = email.value();
            PersistentRepository.LoginResult result = repository.login(attemptedEmail, password.rawValue());
            if (result.status != PersistentRepository.LoginStatus.SUCCESS) {
                isSubmitting = false;
                Motion.restoreButton(login, "Sign in", email.input, password.input, demo, forgot);
                if (result.status == PersistentRepository.LoginStatus.NO_ACCOUNT) {
                    email.error("No account found for this email. Please create an account first.");
                    Motion.shakeError(email.layout);
                } else if (result.status == PersistentRepository.LoginStatus.BAD_CREDENTIALS) {
                    password.error("Invalid credentials. Please check your password and try again.");
                    Motion.shakeError(password.layout);
                } else if (result.status == PersistentRepository.LoginStatus.RECOVERY_REQUIRED) {
                    UiFeedback.showConfirm(
                            this,
                            "Account recovery needed",
                            "This account exists, but no password was ever stored for it on this "
                                    + "device (older builds did not save one). Offline recovery lets "
                                    + "you set a new password; your community data is preserved.",
                            "Open recovery",
                            getString(R.string.action_cancel),
                            false,
                            () -> navigate("RECOVER", attemptedEmail),
                            null);
                } else {
                    UiFeedback.showError(this, "Sign-in failed",
                            safe(result.message, "Please try again."), null);
                }
                return;
            }

            hideKeyboard();
            Motion.completeButtonWithConfirmation(login, getString(R.string.feedback_login_success), "Sign in", () -> {
                isSubmitting = false;
                UiFeedback.showSuccess(
                        this,
                        getString(R.string.feedback_login_success),
                        getString(R.string.feedback_login_success_subtitle),
                        700L,
                        () -> {
                            CommunityRole existingRole = repository.getCurrentUser().role;
                            if (existingRole != null) rootRoute("HOME");
                            else navigate("ROLE_PICKER");
                        }
                );
            }, email.input, password.input, demo, forgot);
        });
        addDisclaimer(page);
        attachAuthPage(page);
    }

    private void showRegistration() {
        LinearLayout page = authPage(true);
        addAuthHeader(page, getString(R.string.register_title), getString(R.string.register_subtitle));
        Field name = createField("Full name", "Your full name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        Field email = createField("Email address", "name@example.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
        Field password = createField("Password", "At least 6 characters", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, false);
        password.layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        Field confirm = createField("Confirm password", "Repeat password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, false);
        confirm.layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        Field unit = createField("Apartment / unit", "For example, C-204", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, false);
        Field phone = createField("Phone (optional)", "Your phone number", InputType.TYPE_CLASS_PHONE, false);

        addTopMargin(page, name.layout, 22);
        addTopMargin(page, email.layout, 12);
        addTopMargin(page, password.layout, 12);
        addTopMargin(page, confirm.layout, 12);
        addTopMargin(page, unit.layout, 12);
        addTopMargin(page, phone.layout, 12);

        for (Field f : new Field[]{name, email, password, confirm, unit, phone}) setupClearErrorOnType(f);

        MaterialButton create = fullButton("Create account", true);
        addTopMargin(page, create, 18);
        create.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean ok = true;

            Validation.Result r;
            r = Validation.validateName(name.value());
            if (!r.valid) { name.error(r.error); ok = false; } else name.clearError();

            r = Validation.validateEmail(email.value());
            if (!r.valid) { email.error(r.error); ok = false; } else {
                if (repository.isEmailRegistered(email.value())) {
                    email.error("Email already registered");
                    ok = false;
                } else email.clearError();
            }

            r = Validation.validatePassword(password.rawValue());
            if (!r.valid) { password.error(r.error); ok = false; } else password.clearError();

            r = Validation.validatePasswordConfirm(password.rawValue(), confirm.rawValue());
            if (!r.valid) { confirm.error(r.error); ok = false; } else confirm.clearError();

            r = Validation.validateUnit(unit.value());
            if (!r.valid) { unit.error(r.error); ok = false; } else unit.clearError();

            r = Validation.validatePhone(phone.value(), false);
            if (!r.valid) { phone.error(r.error); ok = false; } else phone.clearError();

            if (!ok) {
                Motion.shakeError(create);
                return;
            }

            isSubmitting = true;
            Motion.setButtonLoading(create, "Creating account...",
                    name.input, email.input, password.input, confirm.input, unit.input, phone.input);

            String registrationError = repository.registerAccount(
                    name.value(), email.value(), unit.value(), phone.value(), password.rawValue());
            if (registrationError != null) {
                isSubmitting = false;
                Motion.restoreButton(create, "Create account",
                        name.input, email.input, password.input, confirm.input, unit.input, phone.input);
                if (repository.isEmailRegistered(email.value())) {
                    email.error(registrationError);
                    Motion.shakeError(email.layout);
                } else {
                    UiFeedback.showError(this, "Could not create account", registrationError, null);
                }
                return;
            }
            hideKeyboard();
            Motion.completeButtonWithConfirmation(create, getString(R.string.feedback_register_success), "Create account", () -> {
                isSubmitting = false;
                UiFeedback.showSuccess(
                        this,
                        getString(R.string.feedback_register_success),
                        getString(R.string.feedback_register_success_subtitle),
                        750L,
                        () -> navigate("ROLE_PICKER")
                );
            }, name.input, email.input, password.input, confirm.input, unit.input, phone.input);
        });
        addDisclaimer(page);
        attachAuthPage(page);
    }

    private void showForgotPassword() {
        LinearLayout page = authPage(true);
        addAuthHeader(page, getString(R.string.forgot_title), getString(R.string.forgot_subtitle));
        Field email = createField("Email address", "name@example.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
        addTopMargin(page, email.layout, 22);
        setupClearErrorOnType(email);

        MaterialButton send = fullButton("Send reset instructions", true);
        addTopMargin(page, send, 18);
        send.setOnClickListener(v -> {
            if (isSubmitting) return;
            Validation.Result r = Validation.validateEmail(email.value());
            if (!r.valid) {
                email.error(r.error);
                Motion.shakeError(email.layout);
                return;
            }
            email.clearError();
            if (!repository.isEmailRegistered(email.value())) {
                email.error("No account found for this email");
                Motion.shakeError(email.layout);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(send, "Verifying account...", email.input);
            hideKeyboard();
            Motion.completeButtonWithConfirmation(send, "Instructions ready", "Send reset instructions", () -> {
                isSubmitting = false;
                UiFeedback.showSuccess(
                        this,
                        "Reset instructions saved",
                        "Account verified for " + email.value() + ". Local recovery instructions have been recorded on this device.",
                        0L,
                        null
                );
            }, email.input);
        });
        addDisclaimer(page);
        attachAuthPage(page);
    }

    /**
     * Explicit offline account recovery for accounts that never had a password stored
     * (legacy records). Honest about the threat model: identity cannot be verified without a
     * backend, so this flow is only offered for accounts with NO usable credential and requires
     * an informed confirmation. Accounts that already have a password cannot be reset offline.
     */
    private void showRecovery(String presetEmail) {
        LinearLayout page = authPage(true);
        addAuthHeader(page, "Account recovery", "Set a password for a local account that never had one");
        TextView info = text("Offline recovery — this app has no backend, so your identity cannot be "
                + "verified by email. If this account has no password stored on this device (legacy "
                + "accounts from older builds), you can set a new password here and its community "
                + "data is preserved. Accounts that already have a password cannot be reset offline.",
                13, R.color.text_secondary, false);
        info.setLineSpacing(dp(3), 1f);
        info.setPadding(dp(14), dp(12), dp(14), dp(12));
        info.setBackground(shape(color(R.color.warning_container), dp(14), color(R.color.warning_container), 0));
        addTopMargin(page, info, 18);

        Field email = createField("Email address", "name@example.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
        if (presetEmail != null && !presetEmail.trim().isEmpty()) email.input.setText(presetEmail.trim());
        Field password = createField("New password", "At least 6 characters", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, false);
        password.layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        Field confirm = createField("Confirm password", "Repeat password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, false);
        confirm.layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        addTopMargin(page, email.layout, 18);
        addTopMargin(page, password.layout, 12);
        addTopMargin(page, confirm.layout, 12);
        for (Field f : new Field[]{email, password, confirm}) setupClearErrorOnType(f);

        MaterialButton recover = fullButton("Set password and sign in", true);
        addTopMargin(page, recover, 18);
        recover.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean ok = true;
            Validation.Result r = Validation.validateEmail(email.value());
            if (!r.valid) {
                email.error(r.error);
                ok = false;
            } else if (!repository.isEmailRegistered(email.value())) {
                email.error("No account found for this email on this device.");
                ok = false;
            } else {
                email.clearError();
            }
            r = Validation.validatePassword(password.rawValue());
            if (!r.valid) { password.error(r.error); ok = false; } else password.clearError();
            r = Validation.validatePasswordConfirm(password.rawValue(), confirm.rawValue());
            if (!r.valid) { confirm.error(r.error); ok = false; } else confirm.clearError();
            if (!ok) {
                Motion.shakeError(recover);
                return;
            }

            UiFeedback.showConfirm(
                    this,
                    "Set a new password?",
                    "Offline recovery cannot verify your identity. This sets a new password for the "
                            + "local account " + email.value() + ". Its community data is preserved.",
                    "Set password",
                    getString(R.string.action_cancel),
                    true,
                    () -> {
                        if (isSubmitting) return;
                        isSubmitting = true;
                        Motion.setButtonLoading(recover, "Updating credentials...",
                                email.input, password.input, confirm.input);
                        hideKeyboard();
                        String recoveryError = repository.recoverAccountSetPassword(
                                email.value(), password.rawValue());
                        if (recoveryError != null) {
                            isSubmitting = false;
                            Motion.restoreButton(recover, "Set password and sign in",
                                    email.input, password.input, confirm.input);
                            UiFeedback.showError(this, "Recovery not possible", recoveryError, null);
                            return;
                        }
                        PersistentRepository.LoginResult loginResult =
                                repository.login(email.value(), password.rawValue());
                        if (loginResult.status != PersistentRepository.LoginStatus.SUCCESS) {
                            isSubmitting = false;
                            Motion.restoreButton(recover, "Set password and sign in",
                                    email.input, password.input, confirm.input);
                            UiFeedback.showError(this, "Sign-in failed after recovery",
                                    safe(loginResult.message, "Please try signing in again."), null);
                            return;
                        }
                        Motion.completeButtonWithConfirmation(recover, "Password set",
                                "Set password and sign in", () -> {
                                    isSubmitting = false;
                                    UiFeedback.showSuccess(
                                            this,
                                            "Account recovered",
                                            "A new password was set and you are signed in.",
                                            750L,
                                            () -> {
                                                if (repository.getCurrentUser().role != null) rootRoute("HOME");
                                                else rootRoute("ROLE_PICKER");
                                            });
                                }, email.input, password.input, confirm.input);
                    },
                    null);
        });
        addDisclaimer(page);
        attachAuthPage(page);
    }

    private void showRolePicker() {
        LinearLayout page = authPage(true);
        addAuthHeader(page, getString(R.string.role_picker_title), getString(R.string.role_picker_subtitle));
        // Least privilege: ordinary accounts may only choose Resident. Staff roles require a
        // grant from a Management account; the one exception is the first-ever role selection on
        // a device that has no staff account yet (device-owner bootstrap). The repository
        // enforces the same rules, so bypassing this screen does not bypass the policy.
        final boolean deviceSetup = repository.isDeviceOwnerSetupAvailable();
        List<CommunityRole> options = new ArrayList<>();
        if (deviceSetup) {
            Collections.addAll(options, CommunityRole.values());
        } else {
            options.add(CommunityRole.RESIDENT);
        }
        String infoText = deviceSetup
                ? "Device owner setup: no staff account exists on this device yet, so this account "
                  + "may choose any role once. Afterwards, Management, Security and Maintenance "
                  + "roles can only be granted by a Management account under Settings → Staff "
                  + "access. These are on-device checks — not production security."
                : "Your account signs in as a Resident. Management, Security and Maintenance roles "
                  + "must be granted by a Management account (Settings → Staff access). These are "
                  + "on-device checks — not production security.";
        TextView info = text(infoText, 13, R.color.text_secondary, false);
        info.setLineSpacing(dp(3), 1f);
        info.setPadding(dp(14), dp(12), dp(14), dp(12));
        info.setBackground(shape(color(R.color.primary_container), dp(14), color(R.color.primary_container), 0));
        addTopMargin(page, info, 18);
        List<View> roleCards = new ArrayList<>();
        for (CommunityRole role : options) {
            MaterialCardView card = card();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));

            ImageView icon = new ImageView(this);
            icon.setImageResource(roleIcon(role));
            icon.setColorFilter(color(R.color.primary));
            icon.setPadding(dp(11), dp(11), dp(11), dp(11));
            icon.setBackground(shape(color(R.color.primary_light), dp(14), color(R.color.primary_light), 0));
            icon.setContentDescription(null);
            row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            copyParams.leftMargin = dp(14);
            row.addView(copy, copyParams);
            TextView title = text(role.getLabel(), 16, R.color.text_primary, true);
            copy.addView(title);
            TextView desc = text(role.getDescription(), 13, R.color.text_secondary, false);
            addTopMargin(copy, desc, 3);

            ImageView chevron = new ImageView(this);
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setColorFilter(color(R.color.text_muted));
            chevron.setContentDescription(null);
            row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));

            card.addView(row);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(12);
            page.addView(card, cardParams);
            roleCards.add(card);
            makeClickable(card, "Select " + role.getLabel(), () -> {
                String roleError = repository.chooseOwnRole(role);
                if (roleError != null) {
                    UiFeedback.showError(this, "Role not available", roleError, null);
                    return;
                }
                rootRoute("HOME");
                Toast.makeText(this, role.getLabel() + " selected", Toast.LENGTH_SHORT).show();
            });
        }
        attachAuthPage(page, false);
        if (animateCurrentRender) Motion.staggerEnter(roleCards);
        addDisclaimer(page);
    }

    @DrawableRes
    private int roleIcon(CommunityRole role) {
        switch (role) {
            case MANAGEMENT: return R.drawable.ic_briefcase;
            case SECURITY: return R.drawable.ic_shield;
            case MAINTENANCE: return R.drawable.ic_build;
            default: return R.drawable.ic_home;
        }
    }

    private void makeClickable(MaterialCardView card, String description, Runnable action) {
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(description);
        card.setOnClickListener(v -> action.run());
        Motion.attachPressFeedback(card);
    }

    private LinearLayout authPage(boolean withBack) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(14), dp(24), dp(26));
        page.setBackgroundColor(color(R.color.background));
        if (withBack) {
            ImageButton back = iconButton(R.drawable.ic_back, "Back");
            back.setOnClickListener(v -> onBackPressed());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(44));
            params.bottomMargin = dp(8);
            page.addView(back, params);
        }
        return page;
    }

    private void attachAuthPage(LinearLayout page) {
        attachAuthPage(page, animateCurrentRender);
    }

    private void attachAuthPage(LinearLayout page, boolean animate) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.addView(page, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screenRoot.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (animate) Motion.enter(page, 0L);
    }

    private void addBranding(LinearLayout parent) {
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_app_logo);
        mark.setContentDescription(getString(R.string.app_name));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        copyParams.leftMargin = dp(12);
        brand.addView(copy, copyParams);
        copy.addView(text("CEDAR GROVE", 13, R.color.primary_dark, true));
        TextView subtitle = text("SMART COMMUNITY", 10, R.color.text_muted, true);
        subtitle.setLetterSpacing(0.1f);
        addTopMargin(copy, subtitle, 3);
        parent.addView(brand);
    }

    private void addAuthHeader(LinearLayout page, String heading, String subheading) {
        addBranding(page);
        TextView title = text(heading, 27, R.color.text_primary, true);
        addTopMargin(page, title, 24);
        TextView subtitle = text(subheading, 14, R.color.text_secondary, false);
        subtitle.setLineSpacing(dp(3), 1f);
        addTopMargin(page, subtitle, 7);
    }

    private void addDisclaimer(LinearLayout parent) {
        TextView disclaimer = text(getString(R.string.demo_disclaimer), 11, R.color.text_muted, false);
        disclaimer.setGravity(Gravity.CENTER);
        disclaimer.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(26);
        parent.addView(disclaimer, params);
    }

    // --- HOME ---

    private void showHome(FrameLayout panel) {
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout content = verticalContent();
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll);

        User user = repository.getCurrentUser();
        String firstName = user.name == null || user.name.trim().isEmpty() ? "there" : user.name.trim().split("\\s+")[0];
        TextView greeting = text("Welcome, " + firstName + ".", 27, R.color.text_primary, true);
        content.addView(greeting);
        TextView subtitle = text(user.role.getLabel() + " · " + safe(user.apartmentUnit, "Community"), 14, R.color.text_secondary, false);
        addTopMargin(content, subtitle, 5);

        MaterialCardView banner = card();
        banner.setCardBackgroundColor(color(R.color.primary_container));
        LinearLayout bannerCopy = new LinearLayout(this);
        bannerCopy.setOrientation(LinearLayout.VERTICAL);
        bannerCopy.setPadding(dp(18), dp(16), dp(18), dp(16));
        bannerCopy.addView(text("A better connected community", 16, R.color.primary_dark, true));
        TextView bannerNote = text("Find updates, requests and helpful contacts in one place. All data is stored locally and will sync when backend is available.", 13, R.color.text_secondary, false);
        bannerNote.setLineSpacing(dp(2), 1f);
        addTopMargin(bannerCopy, bannerNote, 6);
        banner.addView(bannerCopy);
        addTopMargin(content, banner, 22);

        addSectionHeader(content, summaryHeading(), null, null, 22);
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        List<String[]> values = summaryValues(user.role);
        for (int i = 0; i < values.size(); i++) {
            String[] value = values.get(i);
            MaterialCardView stat = card();
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(14), dp(14), dp(12), dp(14));
            box.addView(text(value[0], 24, R.color.primary, true));
            TextView label = text(value[1], 11, R.color.text_secondary, true);
            addTopMargin(box, label, 3);
            stat.addView(box);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) params.leftMargin = dp(8);
            stats.addView(stat, params);
        }
        addTopMargin(content, stats, 10);

        addSectionHeader(content, "Quick access", null, null, 24);
        addActionGrid(content, homeTiles(user.role));

        addSectionHeader(content, "Recent community updates", "See all", () -> navigate("ANNOUNCEMENTS"), 24);
        List<Announcement> recent = repository.getAnnouncements();
        for (int i = 0; i < Math.min(2, recent.size()); i++) {
            Announcement announcement = recent.get(i);
            MaterialCardView update = compactInfoCard(announcement.title,
                    announcement.category + " · " + announcement.createdAt,
                    announcement.read ? "Read" : "Unread");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(8);
            content.addView(update, params);
            makeClickable(update, announcement.title,
                    () -> {
                        repository.markAnnouncementRead(announcement.id, true);
                        navigate("DETAIL_ANNOUNCEMENT", announcement.id);
                    });
        }

        CommunityEvent upcoming = null;
        for (CommunityEvent event : repository.getEvents()) if (event.upcoming) { upcoming = event; break; }
        final CommunityEvent upcomingEvent = upcoming;
        if (upcomingEvent != null) {
            addSectionHeader(content, "Coming up", "All events", () -> navigate("EVENTS"), 24);
            MaterialCardView eventCard = compactInfoCard(upcomingEvent.title, upcomingEvent.eventDateTime + " · " + upcomingEvent.location,
                    upcomingEvent.attending ? "Going" : "Event");
            content.addView(eventCard);
            makeClickable(eventCard, upcomingEvent.title, () -> navigate("DETAIL_EVENT", upcomingEvent.id));
        }
        TextView footer = text("Data is persisted locally. Backend sync pending: Spring Boot + MySQL.", 11, R.color.text_muted, false);
        footer.setGravity(Gravity.CENTER);
        addTopMargin(content, footer, 20);
    }

    private String summaryHeading() {
        CommunityRole role = repository.getCurrentUser().role;
        if (role == CommunityRole.SECURITY) return "Gate overview";
        if (role == CommunityRole.MAINTENANCE) return "Maintenance overview";
        if (role == CommunityRole.MANAGEMENT) return "Community overview";
        return "Your community at a glance";
    }

    private List<String[]> summaryValues(CommunityRole role) {
        List<String[]> result = new ArrayList<>();
        if (role == CommunityRole.SECURITY) {
            int in = 0;
            for (Visitor item : repository.getVisitors()) if ("Checked in".equals(item.status)) in++;
            result.add(new String[]{String.valueOf(repository.countExpectedVisitors()), "Expected"});
            result.add(new String[]{String.valueOf(in), "On site"});
            result.add(new String[]{String.valueOf(repository.countUnreadNotifications()), "Updates"});
        } else if (role == CommunityRole.MAINTENANCE) {
            int open = 0, progress = 0, resolved = 0;
            for (Complaint item : visibleComplaints()) {
                if (!"Resolved".equals(item.status)) open++;
                if ("In Progress".equals(item.status)) progress++;
                if ("Resolved".equals(item.status)) resolved++;
            }
            result.add(new String[]{String.valueOf(open), "Open"});
            result.add(new String[]{String.valueOf(progress), "In progress"});
            result.add(new String[]{String.valueOf(resolved), "Resolved"});
        } else {
            int open = 0;
            for (Complaint item : visibleComplaints()) if (!"Resolved".equals(item.status)) open++;
            result.add(new String[]{String.valueOf(open), "Open issues"});
            result.add(new String[]{String.valueOf(repository.countExpectedVisitors()), "Visitors"});
            result.add(new String[]{String.valueOf(repository.countUnreadAnnouncements()), "New updates"});
        }
        return result;
    }

    private List<Tile> homeTiles(CommunityRole role) {
        List<Tile> tiles = new ArrayList<>();
        if (role == CommunityRole.SECURITY) {
            tiles.add(new Tile("Visitors", "Expected and recent", "VISITORS", R.drawable.ic_visitors));
            tiles.add(new Tile("Emergency contacts", "Quick dial directory", "EMERGENCY", R.drawable.ic_phone));
            tiles.add(new Tile("Announcements", "Community notices", "ANNOUNCEMENTS", R.drawable.ic_bell));
            tiles.add(new Tile("Notifications", "Inbox", "NOTIFICATIONS", R.drawable.ic_bell));
        } else if (role == CommunityRole.MAINTENANCE) {
            tiles.add(new Tile("Complaints", "Review work queue", "COMPLAINTS", R.drawable.ic_build));
            tiles.add(new Tile("Operations feed", "Assigned work history", "OPS_FEED", R.drawable.ic_briefcase));
            tiles.add(new Tile("Asset reliability", "Prior incidents and repairs", "OPS_ASSETS", R.drawable.ic_build));
            tiles.add(new Tile("Resolution quality", "Proposed versus verified", "OPS_QUALITY", R.drawable.ic_check));
            tiles.add(new Tile("Service requests", "View request status", "REQUESTS", R.drawable.ic_calendar));
            tiles.add(new Tile("Announcements", "Community notices", "ANNOUNCEMENTS", R.drawable.ic_bell));
            tiles.add(new Tile("Emergency contacts", "Directory", "EMERGENCY", R.drawable.ic_phone));
            tiles.add(new Tile("Notifications", "Inbox", "NOTIFICATIONS", R.drawable.ic_bell));
        } else {
            tiles.add(new Tile("Announcements", "Notices and updates", "ANNOUNCEMENTS", R.drawable.ic_bell));
            tiles.add(new Tile("Complaints", "Report and track", "COMPLAINTS", R.drawable.ic_build));
            tiles.add(new Tile("Issue activity", "Your outcome timeline", "OPS_FEED", R.drawable.ic_briefcase));
            tiles.add(new Tile("Resolution quality", "Track verified outcomes", "OPS_QUALITY", R.drawable.ic_check));
            if (role == CommunityRole.MANAGEMENT) {
                tiles.add(new Tile("Asset reliability", "Service history", "OPS_ASSETS", R.drawable.ic_build));
            }
            tiles.add(new Tile("Service requests", "Community help", "REQUESTS", R.drawable.ic_calendar));
            tiles.add(new Tile("Visitors", "Register visits", "VISITORS", R.drawable.ic_visitors));
            tiles.add(new Tile("Resident directory", "Find neighbours", "RESIDENTS", R.drawable.ic_people));
            tiles.add(new Tile("Events", "What's happening", "EVENTS", R.drawable.ic_calendar));
            tiles.add(new Tile("Emergency contacts", "Important numbers", "EMERGENCY", R.drawable.ic_phone));
            tiles.add(new Tile("Documents", "Resources", "DOCUMENTS", R.drawable.ic_people));
            tiles.add(new Tile("Notifications", "Your inbox", "NOTIFICATIONS", R.drawable.ic_bell));
        }
        return tiles;
    }

    private void showCommunityHub(FrameLayout panel) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = verticalContent();
        scroll.addView(content);
        panel.addView(scroll);
        content.addView(text("Everything shared by your community.", 15, R.color.text_secondary, false));
        TextView communityNote = text("Updates, neighbours and resources for Cedar Grove.", 13, R.color.text_muted, false);
        addTopMargin(content, communityNote, 4);
        List<Tile> tiles = new ArrayList<>();
        CommunityRole role = repository.getCurrentUser().role;
        tiles.add(new Tile("Announcements", "Notices from management", "ANNOUNCEMENTS", R.drawable.ic_bell));
        if (role != CommunityRole.SECURITY) tiles.add(new Tile("Residents", "Search directory", "RESIDENTS", R.drawable.ic_people));
        tiles.add(new Tile("Events", "Upcoming gatherings", "EVENTS", R.drawable.ic_calendar));
        if (role != CommunityRole.SECURITY) tiles.add(new Tile("Documents", "Community resources", "DOCUMENTS", R.drawable.ic_people));
        tiles.add(new Tile("Emergency contacts", "Public and community", "EMERGENCY", R.drawable.ic_phone));
        tiles.add(new Tile("Notifications", "Read and unread", "NOTIFICATIONS", R.drawable.ic_bell));
        addSectionHeader(content, "Explore", null, null, 22);
        addActionGrid(content, tiles);
    }

    private void showWorkHub(FrameLayout panel) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = verticalContent();
        scroll.addView(content);
        panel.addView(scroll);
        CommunityRole role = repository.getCurrentUser().role;
        String intro = role == CommunityRole.SECURITY
                ? "A focused view of expected visitors and essential contacts."
                : role == CommunityRole.MAINTENANCE
                ? "Review issues, update work status, and check incoming requests."
                : role == CommunityRole.MANAGEMENT
                ? "Review community operations and manage announcements, events and residents."
                : "Request help and keep track of your community services.";
        content.addView(text(intro, 15, R.color.text_secondary, false));
        List<Tile> tiles = new ArrayList<>();
        if (role == CommunityRole.SECURITY) {
            tiles.add(new Tile("Visitor overview", "Expected and checked-in", "VISITORS", R.drawable.ic_visitors));
            tiles.add(new Tile("Emergency contacts", "Contact directory", "EMERGENCY", R.drawable.ic_phone));
        } else if (role == CommunityRole.MAINTENANCE) {
            tiles.add(new Tile("Complaint queue", "Update work status", "COMPLAINTS", R.drawable.ic_build));
            tiles.add(new Tile("Service requests", "Review requests", "REQUESTS", R.drawable.ic_calendar));
        } else {
            tiles.add(new Tile("Complaints", "Submit or review issues", "COMPLAINTS", R.drawable.ic_build));
            tiles.add(new Tile("Service requests", "Submit or track requests", "REQUESTS", R.drawable.ic_calendar));
            tiles.add(new Tile("Visitors", "Register and review", "VISITORS", R.drawable.ic_visitors));
            if (role == CommunityRole.MANAGEMENT) {
                tiles.add(new Tile("Resident directory", "Manage residents", "RESIDENTS", R.drawable.ic_people));
                tiles.add(new Tile("Announcements", "Publish notices", "ANNOUNCEMENTS", R.drawable.ic_bell));
                tiles.add(new Tile("Events", "Create events", "EVENTS", R.drawable.ic_calendar));
            }
            tiles.add(new Tile("Emergency contacts", "Contact directory", "EMERGENCY", R.drawable.ic_phone));
        }
        addSectionHeader(content, "Services", null, null, 22);
        addActionGrid(content, tiles);
    }

    private void showProfile(FrameLayout panel) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = verticalContent();
        scroll.addView(content);
        panel.addView(scroll);
        User user = repository.getCurrentUser();
        MaterialCardView userCard = card();
        LinearLayout userInfo = new LinearLayout(this);
        userInfo.setGravity(Gravity.CENTER_VERTICAL);
        userInfo.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView avatar = text(initials(user.name), 20, R.color.white, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(color(R.color.primary), dp(50), color(R.color.primary), 0));
        userInfo.addView(avatar, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout nameBlock = new LinearLayout(this);
        nameBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = dp(14);
        userInfo.addView(nameBlock, nameParams);
        nameBlock.addView(text(safe(user.name, "User"), 18, R.color.text_primary, true));
        TextView role = text(user.role.getLabel(), 13, R.color.primary, true);
        addTopMargin(nameBlock, role, 4);
        userCard.addView(userInfo);
        content.addView(userCard);

        addSectionHeader(content, "Personal information", null, null, 24);
        MaterialCardView details = card();
        LinearLayout detailsColumn = new LinearLayout(this);
        detailsColumn.setOrientation(LinearLayout.VERTICAL);
        detailsColumn.setPadding(dp(18), dp(16), dp(18), dp(16));
        addKeyValue(detailsColumn, "Email", safe(user.email, "Not set"));
        addKeyValue(detailsColumn, "Phone", safe(user.phone, "Not set"));
        addKeyValue(detailsColumn, "Apartment", safe(user.apartmentUnit, "Not set"));
        addKeyValue(detailsColumn, "Current role", user.role.getLabel());
        details.addView(detailsColumn);
        content.addView(details);

        MaterialButton edit = fullButton("Edit profile", true);
        addTopMargin(content, edit, 16);
        edit.setOnClickListener(v -> navigate("EDIT_PROFILE"));
        MaterialButton settings = fullButton("Settings and preferences", false);
        addTopMargin(content, settings, 10);
        settings.setOnClickListener(v -> navigate("SETTINGS"));
        MaterialButton logout = fullButton("Sign out", false);
        logout.setTextColor(color(R.color.danger));
        addTopMargin(content, logout, 10);
        logout.setOnClickListener(v -> confirmLogout());
    }

    private void showSettings(FrameLayout panel) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = verticalContent();
        scroll.addView(content);
        panel.addView(scroll);
        TextView intro = text("Manage local preferences and backend configuration.", 14, R.color.text_secondary, false);
        intro.setLineSpacing(dp(2), 1f);
        content.addView(intro);
        MaterialCardView card = card();
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(6), dp(16), dp(6));
        addPreferenceSwitch(rows, "Community inbox", "Show local notifications", "pref_demo_inbox");
        addPreferenceSwitch(rows, "Event reminders", "Enable event reminders", "pref_demo_events");
        card.addView(rows);
        addTopMargin(content, card, 18);

        addSectionHeader(content, "Backend", null, null, 24);
        MaterialCardView backendCard = card();
        LinearLayout backendInfo = new LinearLayout(this);
        backendInfo.setOrientation(LinearLayout.VERTICAL);
        backendInfo.setPadding(dp(17), dp(16), dp(17), dp(16));
        backendInfo.addView(text("API Configuration", 15, R.color.text_primary, true));
        TextView apiStatus = text("Backend: " + (ApiConfig.isBackendAvailable(this) ? "Connected" : "Local only (Spring Boot pending)"), 13, R.color.text_secondary, false);
        addTopMargin(backendInfo, apiStatus, 7);
        TextView baseUrl = text("Base URL: " + ApiConfig.getBaseUrl(this), 12, R.color.text_muted, false);
        addTopMargin(backendInfo, baseUrl, 4);
        backendCard.addView(backendInfo);
        addTopMargin(content, backendCard, 10);

        if (repository.getCurrentUser().role == CommunityRole.MANAGEMENT) {
            addStaffAccessSection(content);
        }

        addSectionHeader(content, "About", null, null, 24);
        MaterialCardView about = card();
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(17), dp(16), dp(17), dp(16));
        LinearLayout aboutHeader = new LinearLayout(this);
        aboutHeader.setOrientation(LinearLayout.HORIZONTAL);
        aboutHeader.setGravity(Gravity.CENTER_VERTICAL);
        ImageView aboutLogo = new ImageView(this);
        aboutLogo.setImageResource(R.drawable.ic_app_logo);
        aboutLogo.setContentDescription(getString(R.string.app_name));
        LinearLayout.LayoutParams aboutLogoParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        aboutLogoParams.rightMargin = dp(12);
        aboutHeader.addView(aboutLogo, aboutLogoParams);
        aboutHeader.addView(text("Smart Apartment Community", 15, R.color.text_primary, true));
        info.addView(aboutHeader);
        TextView body = text("Native Android Java/XML · Persistent local storage · Blue/teal Stitch-inspired design · Future: Spring Boot + MySQL backend with secure auth and sync.", 13, R.color.text_secondary, false);
        body.setLineSpacing(dp(3), 1f);
        addTopMargin(info, body, 8);
        about.addView(info);
        addTopMargin(content, about, 10);
        MaterialButton logout = fullButton("Sign out", false);
        logout.setTextColor(color(R.color.danger));
        addTopMargin(content, logout, 20);
        logout.setOnClickListener(v -> confirmLogout());
    }

    /**
     * Management-only, device-local staff access panel: grants or revokes roles for the other
     * accounts registered on this device. Enforcement happens in the repository/AccountStore
     * (this panel is only reachable by a MANAGEMENT session, and the grant call re-checks it).
     * Client-side, single-device control — NOT server-grade authorization.
     */
    private void addStaffAccessSection(LinearLayout content) {
        addSectionHeader(content, "Staff access", null, null, 24);
        MaterialCardView staffCard = card();
        LinearLayout staffColumn = new LinearLayout(this);
        staffColumn.setOrientation(LinearLayout.VERTICAL);
        staffColumn.setPadding(dp(17), dp(16), dp(17), dp(16));
        TextView staffNote = text("Grant or revoke roles for accounts registered on this device. "
                + "Ordinary accounts sign in as Resident. On-device checks like these are not "
                + "production authorization.", 12, R.color.text_secondary, false);
        staffNote.setLineSpacing(dp(3), 1f);
        staffColumn.addView(staffNote);

        final CommunityRole[] roles = CommunityRole.values();
        final String[] roleLabels = new String[roles.length];
        for (int i = 0; i < roles.length; i++) roleLabels[i] = roles[i].getLabel();
        String selfEmail = repository.getCurrentUser().email == null
                ? "" : repository.getCurrentUser().email.trim();
        int shown = 0;
        for (AccountStore.Account account : repository.listAccounts()) {
            if (account.email.equalsIgnoreCase(selfEmail)) continue;
            shown++;
            View divider = new View(this);
            divider.setBackgroundColor(color(R.color.outline));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dividerParams.setMargins(0, dp(12), 0, dp(2));
            staffColumn.addView(divider, dividerParams);

            TextView accountTitle = text(safe(account.name, "Account") + " · " + account.email,
                    14, R.color.text_primary, true);
            addTopMargin(staffColumn, accountTitle, 10);
            String meta = "Unit " + safe(account.unit, "—")
                    + (account.hasValidPassword ? "" : " · no password stored (recovery needed)");
            TextView accountMeta = text(meta, 11, R.color.text_muted, false);
            addTopMargin(staffColumn, accountMeta, 2);

            int selectedIndex = 0;
            CommunityRole effectiveRole = account.role == null ? CommunityRole.RESIDENT : account.role;
            for (int i = 0; i < roles.length; i++) {
                if (roles[i] == effectiveRole) { selectedIndex = i; break; }
            }
            Spinner roleSpinner = addSpinner(staffColumn, "Role", roleLabels);
            roleSpinner.setSelection(selectedIndex);
            MaterialButton apply = smallButton("Apply role", true);
            LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            applyParams.topMargin = dp(6);
            staffColumn.addView(apply, applyParams);
            apply.setOnClickListener(button -> {
                if (isSubmitting) return;
                int position = roleSpinner.getSelectedItemPosition();
                final CommunityRole newRole = roles[position >= 0 && position < roles.length ? position : 0];
                final CommunityRole currentRole = effectiveRole;
                if (newRole == currentRole) {
                    Toast.makeText(this, account.email + " already has the "
                            + newRole.getLabel() + " role.", Toast.LENGTH_SHORT).show();
                    return;
                }
                final boolean grant = newRole != CommunityRole.RESIDENT;
                String message = grant
                        ? "Grant " + newRole.getLabel() + " access to " + account.email + "? The "
                          + "change applies to this device's local account."
                        : "Revoke staff access for " + account.email + "? Their role becomes Resident.";
                UiFeedback.showConfirm(
                        this,
                        grant ? "Grant staff role?" : "Revoke staff role?",
                        message,
                        grant ? "Grant" : "Revoke",
                        getString(R.string.action_cancel),
                        !grant,
                        () -> {
                            String roleError = repository.grantRole(account.email, newRole);
                            if (roleError != null) {
                                UiFeedback.showError(this, "Role not changed", roleError, null);
                            } else {
                                UiFeedback.showSuccess(
                                        this,
                                        grant ? "Role granted" : "Role revoked",
                                        account.email + " is now " + newRole.getLabel() + ".",
                                        700L,
                                        () -> renderQuietly());
                            }
                        },
                        null);
            });
        }
        if (shown == 0) {
            TextView noneText = text("No other local accounts yet. Accounts created through "
                    + "registration appear here.", 12, R.color.text_muted, false);
            addTopMargin(staffColumn, noneText, 10);
        }
        staffCard.addView(staffColumn);
        addTopMargin(content, staffCard, 10);
    }

    private void addPreferenceSwitch(LinearLayout parent, String title, String summary, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.rightMargin = dp(8);
        row.addView(labels, labelsParams);
        labels.addView(text(title, 14, R.color.text_primary, true));
        TextView description = text(summary, 11, R.color.text_secondary, false);
        addTopMargin(labels, description, 3);
        SwitchMaterial toggle = new SwitchMaterial(this);
        String prefKey = "demo_preferences";
        toggle.setChecked(getSharedPreferences(prefKey, MODE_PRIVATE).getBoolean(key, true));
        toggle.setContentDescription(title + " preference");
        toggle.setOnCheckedChangeListener((button, checked) ->
                getSharedPreferences(prefKey, MODE_PRIVATE).edit().putBoolean(key, checked).apply());
        row.addView(toggle);
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (parent.getChildCount() > 1) {
            View divider = new View(this);
            divider.setBackgroundColor(color(R.color.outline));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dividerParams.setMargins(0, dp(7), 0, dp(7));
            parent.addView(divider, parent.getChildCount() - 1, dividerParams);
        }
    }

    private void showList(Route route, FrameLayout panel) {
        String type = route.name;
        final SimpleListAdapter[] adapterHolder = new SimpleListAdapter[1];
        final View[] emptyHolder = new View[1];
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(4), dp(18), dp(8));
        panel.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView intro = text(listIntro(type), 13, R.color.text_secondary, false);
        intro.setLineSpacing(dp(2), 1f);
        page.addView(intro);
        Field search = createField("", searchHint(type), InputType.TYPE_CLASS_TEXT, false);
        search.input.setSingleLine(true);
        search.input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.layout.setHintEnabled(false);
        search.layout.setPlaceholderText(null);
        search.input.setHint(searchHint(type));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.topMargin = dp(12);
        page.addView(search.layout, searchParams);

        String[] choices = filterChoices(type);
        final String[] selectedFilter = new String[]{choices.length == 0 ? "All" : choices[0]};
        final Chip[] firstChipHolder = new Chip[1];
        if (choices.length > 1) {
            HorizontalScrollView filterScroll = new HorizontalScrollView(this);
            filterScroll.setHorizontalScrollBarEnabled(false);
            ChipGroup chips = new ChipGroup(this);
            chips.setSingleLine(true);
            chips.setSingleSelection(true);
            chips.setChipSpacingHorizontal(dp(7));
            for (int i = 0; i < choices.length; i++) {
                String choice = choices[i];
                Chip chip = new Chip(this);
                chip.setId(View.generateViewId());
                chip.setText(choice);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setEnsureMinTouchTargetSize(true);
                chip.setChecked(choice.equals(selectedFilter[0]));
                if (i == 0) firstChipHolder[0] = chip;
                chip.setOnCheckedChangeListener((button, checked) -> {
                    if (checked) {
                        selectedFilter[0] = choice;
                        refreshList(type, search.value(), selectedFilter[0], adapterHolder[0], emptyHolder[0]);
                    }
                });
                chips.addView(chip);
            }
            filterScroll.addView(chips);
            LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            filterParams.topMargin = dp(7);
            page.addView(filterScroll, filterParams);
        }

        FrameLayout listFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        frameParams.topMargin = dp(5);
        page.addView(listFrame, frameParams);
        RecyclerView recycler = new RecyclerView(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setClipToPadding(false);
        recycler.setPadding(0, dp(2), 0, dp(12));

        LinearLayout emptyContainer = new LinearLayout(this);
        emptyContainer.setOrientation(LinearLayout.VERTICAL);
        emptyContainer.setGravity(Gravity.CENTER);
        emptyContainer.setPadding(dp(28), dp(20), dp(28), dp(20));
        emptyContainer.setVisibility(View.GONE);
        TextView empty = text(emptyText(type), 14, R.color.text_secondary, false);
        empty.setGravity(Gravity.CENTER);
        emptyContainer.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        MaterialButton resetEmptyBtn = smallButton("Clear search & filters", false);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetParams.topMargin = dp(12);
        emptyContainer.addView(resetEmptyBtn, resetParams);
        resetEmptyBtn.setOnClickListener(v -> {
            search.input.setText("");
            selectedFilter[0] = choices.length == 0 ? "All" : choices[0];
            if (firstChipHolder[0] != null) firstChipHolder[0].setChecked(true);
            refreshList(type, "", selectedFilter[0], adapterHolder[0], emptyContainer);
        });
        emptyHolder[0] = emptyContainer;

        listFrame.addView(recycler, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listFrame.addView(emptyContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        SimpleListAdapter adapter = new SimpleListAdapter(this, row -> openListRow(type, row.id));
        adapterHolder[0] = adapter;
        recycler.setAdapter(adapter);
        refreshList(type, "", selectedFilter[0], adapter, emptyContainer);
        search.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshList(type, s.toString(), selectedFilter[0], adapter, emptyContainer);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void refreshList(String type, String query, String filter,
                             SimpleListAdapter adapter, View empty) {
        if (adapter == null || empty == null) return;
        List<ListRow> rows = rowsFor(type, query == null ? "" : query.trim(), filter);
        adapter.submitRows(rows);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<ListRow> rowsFor(String type, String query, String filter) {
        List<ListRow> result = new ArrayList<>();
        String q = query.toLowerCase(Locale.getDefault());
        if ("ANNOUNCEMENTS".equals(type)) {
            for (Announcement item : repository.getAnnouncements()) {
                if (matchesFilter(type, item.category, item.priority, filter)
                        && hasQuery(q, item.title, item.description, item.category, item.priority))
                    result.add(new ListRow(item.id, item.title, item.category + " · " + item.priority + " priority · " + snip(item.description, 58),
                            item.createdAt + " · " + (item.read ? "Read" : "Unread"), item.priority));
            }
        } else if ("RESIDENTS".equals(type)) {
            for (Resident item : repository.getResidents()) {
                if (matchesFilter(type, item.apartmentUnit, "", filter)
                        && hasQuery(q, item.name, item.apartmentUnit, item.household))
                    result.add(new ListRow(item.id, item.name, "Apartment " + item.apartmentUnit, item.household + " · Resident", "Resident"));
            }
        } else if ("COMPLAINTS".equals(type)) {
            for (Complaint item : visibleComplaints()) {
                if (matchesFilter(type, item.status, "", filter)
                        && hasQuery(q, item.title, item.description, item.category, item.apartmentUnit, item.priority))
                    result.add(new ListRow(item.id, item.title, item.category + " · " + item.apartmentUnit,
                            item.priority + " priority · " + item.createdAt, item.status));
            }
        } else if ("REQUESTS".equals(type)) {
            for (ServiceRequest item : repository.getRequests()) {
                if (matchesFilter(type, item.status, "", filter)
                        && hasQuery(q, item.title, item.description, item.category, item.apartmentUnit))
                    result.add(new ListRow(item.id, item.title, item.category + " · " + item.apartmentUnit, item.createdAt, item.status));
            }
        } else if ("VISITORS".equals(type)) {
            for (Visitor item : repository.getVisitors()) {
                if (matchesFilter(type, item.status, "", filter)
                        && hasQuery(q, item.name, item.hostUnit, item.purpose, item.expectedArrival))
                    result.add(new ListRow(item.id, item.name, item.purpose + " · Host " + item.hostUnit, item.expectedArrival, item.status));
            }
        } else if ("EVENTS".equals(type)) {
            for (CommunityEvent item : repository.getEvents()) {
                String state = item.upcoming ? "Upcoming" : "Past";
                if (item.attending) state = "Going";
                if (matchesFilter(type, item.upcoming ? "Upcoming" : "Past", "", filter)
                        && hasQuery(q, item.title, item.description, item.location, item.eventDateTime))
                    result.add(new ListRow(item.id, item.title, item.location, item.eventDateTime, state));
            }
        } else if ("EMERGENCY".equals(type)) {
            for (EmergencyContact item : repository.getEmergencyContacts()) {
                if (hasQuery(q, item.name, item.description, item.phone, item.availability))
                    result.add(new ListRow(item.id, item.name, item.description,
                            item.phone.isEmpty() ? item.availability + " · No number" : item.phone + " · " + item.availability,
                            item.phone.isEmpty() ? "Info" : "Call"));
            }
        } else if ("NOTIFICATIONS".equals(type)) {
            for (CommunityNotification item : repository.getNotifications()) {
                if (matchesFilter(type, item.read ? "Read" : "Unread", "", filter)
                        && hasQuery(q, item.title, item.message, item.category))
                    result.add(new ListRow(item.id, item.title, item.message, item.category + " · " + item.createdAt, item.read ? "Read" : "Unread"));
            }
        } else if ("DOCUMENTS".equals(type)) {
            for (CommunityDocument item : repository.getDocuments()) {
                if (matchesFilter(type, item.category, "", filter)
                        && hasQuery(q, item.title, item.category, item.description, item.metadata))
                    result.add(new ListRow(item.id, item.title, item.description, item.metadata, item.category));
            }
        }
        return result;
    }

    private boolean matchesFilter(String type, String primary, String secondary, String filter) {
        if (filter == null || "All".equals(filter)) return true;
        if ("RESIDENTS".equals(type)) return primary.toUpperCase(Locale.US).startsWith(filter.substring(filter.length() - 1));
        if ("EVENTS".equals(type)) return filter.equals(primary);
        if ("ANNOUNCEMENTS".equals(type)) return filter.equals(primary) || filter.equals(secondary);
        return filter.equalsIgnoreCase(primary);
    }

    private boolean hasQuery(String query, String... values) {
        if (query == null || query.isEmpty()) return true;
        for (String value : values) if (value != null && value.toLowerCase(Locale.getDefault()).contains(query)) return true;
        return false;
    }

    private String[] filterChoices(String type) {
        switch (type) {
            case "ANNOUNCEMENTS": return new String[]{"All", "Facilities", "Community", "Events", "Important"};
            case "RESIDENTS": return new String[]{"All", "Tower A", "Tower B", "Tower C"};
            case "COMPLAINTS": return new String[]{"All", "Open", "In Progress", "Resolved"};
            case "REQUESTS": return new String[]{"All", "Open", "Scheduled", "Completed"};
            case "VISITORS": return new String[]{"All", "Expected", "Approved", "Checked in", "Checked out"};
            case "EVENTS": return new String[]{"All", "Upcoming", "Past"};
            case "NOTIFICATIONS": return new String[]{"All", "Unread", "Read"};
            case "DOCUMENTS": return new String[]{"All", "Policies", "Guides", "Forms"};
            default: return new String[]{"All"};
        }
    }

    private String listIntro(String type) {
        switch (type) {
            case "ANNOUNCEMENTS": return "Community notices and updates from management.";
            case "RESIDENTS": return "Search residents by name or apartment unit.";
            case "COMPLAINTS": return repository.getCurrentUser().role == CommunityRole.MAINTENANCE
                    ? "Review assigned issues and update work status." : "Report a maintenance issue and track its status.";
            case "REQUESTS": return "Submit a service request or track existing requests.";
            case "VISITORS": return repository.getCurrentUser().role == CommunityRole.SECURITY
                    ? "Expected visitors and entry status." : "Register expected visits and review entries.";
            case "EVENTS": return "See community gatherings and manage your attendance.";
            case "EMERGENCY": return "Public helplines and verified community contacts.";
            case "NOTIFICATIONS": return "Your community inbox — updates and alerts.";
            case "DOCUMENTS": return "Community resources and documents.";
            default: return "Community information.";
        }
    }

    private String searchHint(String type) {
        switch (type) {
            case "ANNOUNCEMENTS": return "Search announcements";
            case "RESIDENTS": return "Search name or unit";
            case "COMPLAINTS": return "Search issue, category or unit";
            case "REQUESTS": return "Search requests";
            case "VISITORS": return "Search visitor, unit or purpose";
            case "EVENTS": return "Search events";
            case "EMERGENCY": return "Search contacts";
            case "NOTIFICATIONS": return "Search notifications";
            case "DOCUMENTS": return "Search documents";
            default: return "Search";
        }
    }

    private String emptyText(String type) {
        switch (type) {
            case "ANNOUNCEMENTS": return "No announcements match. Clear search or create a new one.";
            case "RESIDENTS": return "No residents match. Try another name or unit.";
            case "COMPLAINTS": return "No complaints match. Clear search or submit a new complaint.";
            case "REQUESTS": return "No requests match. Clear search or create a request.";
            case "VISITORS": return "No visitor records match. Clear search or register a visit.";
            case "EVENTS": return "No events match this filter.";
            case "EMERGENCY": return "No contacts match your search.";
            case "NOTIFICATIONS": return "Your inbox is clear for this filter.";
            case "DOCUMENTS": return "No documents match this search.";
            default: return "Nothing to show yet.";
        }
    }

    private void openListRow(String type, String id) {
        if ("ANNOUNCEMENTS".equals(type)) repository.markAnnouncementRead(id, true);
        if ("NOTIFICATIONS".equals(type)) repository.markNotificationRead(id, true);
        String destination;
        switch (type) {
            case "ANNOUNCEMENTS": destination = "DETAIL_ANNOUNCEMENT"; break;
            case "RESIDENTS": destination = "DETAIL_RESIDENT"; break;
            case "COMPLAINTS": destination = "DETAIL_COMPLAINT"; break;
            case "REQUESTS": destination = "DETAIL_REQUEST"; break;
            case "VISITORS": destination = "DETAIL_VISITOR"; break;
            case "EVENTS": destination = "DETAIL_EVENT"; break;
            case "EMERGENCY": destination = "DETAIL_EMERGENCY"; break;
            case "NOTIFICATIONS": destination = "DETAIL_NOTIFICATION"; break;
            case "DOCUMENTS": destination = "DETAIL_DOCUMENT"; break;
            default: return;
        }
        navigate(destination, id);
    }

    private void showDetail(Route route, FrameLayout panel) {
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout content = verticalContent();
        scroll.addView(content);
        panel.addView(scroll);
        switch (route.name) {
            case "DETAIL_ANNOUNCEMENT": showAnnouncementDetail(content, route.id); break;
            case "DETAIL_RESIDENT": showResidentDetail(content, route.id); break;
            case "DETAIL_COMPLAINT": showComplaintDetail(content, route.id); break;
            case "DETAIL_REQUEST": showRequestDetail(content, route.id); break;
            case "DETAIL_VISITOR": showVisitorDetail(content, route.id); break;
            case "DETAIL_EVENT": showEventDetail(content, route.id); break;
            case "DETAIL_EMERGENCY": showEmergencyDetail(content, route.id); break;
            case "DETAIL_NOTIFICATION": showNotificationDetail(content, route.id); break;
            case "DETAIL_DOCUMENT": showDocumentDetail(content, route.id); break;
        }
    }

    private void showAnnouncementDetail(LinearLayout content, String id) {
        Announcement item = repository.findAnnouncement(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, item.priority + " priority · " + item.category);
        addTopMargin(content, text(item.title, 25, R.color.text_primary, true), 12);
        addTopMargin(content, text(item.createdAt + " · Cedar Grove Management", 13, R.color.text_secondary, false), 7);
        addInfoCard(content, "ANNOUNCEMENT", item.description);
        addInfoCard(content, "READ STATUS", item.read ? "Read" : "Unread");
        MaterialButton toggle = fullButton(item.read ? "Mark as unread" : "Mark as read", false);
        addTopMargin(content, toggle, 14);
        toggle.setOnClickListener(v -> {
            boolean markRead = !item.read;
            repository.markAnnouncementRead(item.id, markRead);
            Toast.makeText(this, markRead ? "Marked read" : "Marked unread", Toast.LENGTH_SHORT).show();
            renderQuietly();
        });
    }

    private void showResidentDetail(LinearLayout content, String id) {
        Resident item = repository.findResident(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, "Resident");
        addTopMargin(content, text(item.name, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "APARTMENT", item.apartmentUnit);
        addInfoCard(content, "HOUSEHOLD", item.household);
        addInfoCard(content, "CONTACT", "For privacy, contact details are available via management office.");
    }

    private void showComplaintDetail(LinearLayout content, String id) {
        final Complaint item = findVisibleComplaint(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, item.status + " · " + item.priority + " priority");
        addTopMargin(content, text(item.title, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "COMPLAINT " + item.id, item.category + " · Apartment " + item.apartmentUnit + "\nSubmitted " + item.createdAt + "\nLast updated " + item.updatedAt);
        addInfoCard(content, "DESCRIPTION", item.description);
        addSectionHeader(content, "Status history", null, null, 20);
        for (String entry : item.history) addTimelineRow(content, entry);
        addInfoCard(content, "OUTCOME", item.outcomeStatus.displayLabel()
                + "\nStaff-proposed resolution and resident verification are separate steps.");
        MaterialButton workspace = fullButton("Open resolution workspace", true);
        addTopMargin(content, workspace, 14);
        workspace.setOnClickListener(v -> openOperations(OperationsActivity.SECTION_ISSUE, item.id));
    }

    private void showRequestDetail(LinearLayout content, String id) {
        ServiceRequest item = repository.findRequest(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, item.status);
        addTopMargin(content, text(item.title, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "REQUEST " + item.id, item.category + " · Apartment " + item.apartmentUnit + "\nSubmitted " + item.createdAt);
        addInfoCard(content, "DESCRIPTION", item.description);
        if (repository.getCurrentUser().role == CommunityRole.MANAGEMENT) {
            final String actionLabel = nextRequestAction(item.status);
            MaterialButton action = fullButton(actionLabel, true);
            addTopMargin(content, action, 14);
            action.setOnClickListener(v -> {
                if (isSubmitting) return;
                Runnable performUpdate = () -> {
                    isSubmitting = true;
                    Motion.setButtonLoading(action, "Updating...");
                    repository.advanceRequest(item.id);
                    Motion.completeButtonWithConfirmation(action, "Status: " + item.status, actionLabel, () -> {
                        isSubmitting = false;
                        renderQuietly();
                    });
                };
                if ("Completed".equals(item.status)) {
                    UiFeedback.showConfirm(this, "Reopen request?",
                            "This will move \"" + item.title + "\" back to Open status.",
                            "Reopen", getString(R.string.action_cancel), false, performUpdate, null);
                } else {
                    performUpdate.run();
                }
            });
        } else {
            addInfoCard(content, "STATUS", "Request status is updated by management team.");
        }
    }

    private void showVisitorDetail(LinearLayout content, String id) {
        Visitor item = repository.findVisitor(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, item.status);
        addTopMargin(content, text(item.name, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "VISIT DETAILS", "Host apartment " + item.hostUnit + "\nPurpose: " + item.purpose
                + "\nExpected: " + item.expectedArrival
                + "\nPhone: " + (item.phone == null || item.phone.isEmpty() ? "Not provided" : item.phone));
        addSectionHeader(content, "Entry history", null, null, 18);
        for (String entry : item.history) addTimelineRow(content, entry);
        CommunityRole role = repository.getCurrentUser().role;
        if (role == CommunityRole.SECURITY || role == CommunityRole.MANAGEMENT) {
            final String actionLabel = nextVisitorAction(item.status);
            MaterialButton action = fullButton(actionLabel, true);
            addTopMargin(content, action, 14);
            action.setOnClickListener(v -> {
                if (isSubmitting) return;
                Runnable performUpdate = () -> {
                    isSubmitting = true;
                    Motion.setButtonLoading(action, "Updating...");
                    repository.advanceVisitor(item.id);
                    Motion.completeButtonWithConfirmation(action, "Status: " + item.status, actionLabel, () -> {
                        isSubmitting = false;
                        renderQuietly();
                    });
                };
                if ("Checked out".equals(item.status)) {
                    UiFeedback.showConfirm(this, "Reset visitor status?",
                            "Reset " + item.name + " back to Expected arrival status?",
                            "Reset status", getString(R.string.action_cancel), false, performUpdate, null);
                } else {
                    performUpdate.run();
                }
            });
        } else {
            addInfoCard(content, "VISITOR STATUS", "Entry status is managed by security desk.");
        }
    }

    private void showEventDetail(LinearLayout content, String id) {
        CommunityEvent item = repository.findEvent(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, item.upcoming ? (item.attending ? "You're going" : "Upcoming") : "Past event");
        addTopMargin(content, text(item.title, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "WHEN AND WHERE", item.eventDateTime + "\n" + item.location);
        addInfoCard(content, "ABOUT THIS EVENT", item.description);
        if (item.upcoming) {
            final String buttonLabel = item.attending ? "Cancel RSVP" : "I'm interested";
            MaterialButton rsvp = fullButton(buttonLabel, !item.attending);
            if (item.attending) {
                rsvp.setTextColor(color(R.color.danger));
            }
            addTopMargin(content, rsvp, 14);
            rsvp.setOnClickListener(v -> {
                if (isSubmitting) return;
                Runnable updateAttendance = () -> {
                    isSubmitting = true;
                    boolean attending = !item.attending;
                    Motion.setButtonLoading(rsvp, attending ? "Saving RSVP..." : "Cancelling...");
                    repository.setEventAttendance(item.id, attending);
                    Motion.completeButtonWithConfirmation(rsvp,
                            attending ? "RSVP saved" : "RSVP cancelled",
                            buttonLabel,
                            () -> {
                                isSubmitting = false;
                                renderQuietly();
                            });
                };
                if (item.attending) {
                    UiFeedback.showConfirm(this, "Cancel RSVP?",
                            "Are you sure you want to cancel your RSVP for \"" + item.title + "\"?",
                            "Cancel RSVP", getString(R.string.action_cancel), true, updateAttendance, null);
                } else {
                    updateAttendance.run();
                }
            });
        }
    }

    private void showEmergencyDetail(LinearLayout content, String id) {
        EmergencyContact item = repository.findEmergencyContact(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, item.availability);
        addTopMargin(content, text(item.name, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "CONTACT", item.description + (item.phone.isEmpty() ? "\nNo phone configured." : "\n" + item.phone));
        if (isDialable(item.phone)) {
            MaterialButton call = fullButton("Call · " + item.phone, true);
            call.setIconResource(R.drawable.ic_phone);
            addTopMargin(content, call, 14);
            call.setOnClickListener(v -> openDialer(item.phone));
        } else {
            addInfoCard(content, "CALL ACTION", "No valid phone number available.");
        }
    }

    private void showNotificationDetail(LinearLayout content, String id) {
        CommunityNotification item = repository.findNotification(id);
        if (item == null) { addNotFound(content); return; }
        repository.markNotificationRead(id, true);
        addPill(content, item.category + " · Read");
        addTopMargin(content, text(item.title, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "MESSAGE", item.message + "\n" + item.createdAt);
        String destination = item.destination;
        if (destination != null && !destination.isEmpty()) {
            MaterialButton open = fullButton("Open related module", true);
            addTopMargin(content, open, 14);
            open.setOnClickListener(v -> navigate(destination));
        }
    }

    private void showDocumentDetail(LinearLayout content, String id) {
        CommunityDocument item = repository.findDocument(id);
        if (item == null) { addNotFound(content); return; }
        addPill(content, item.category);
        addTopMargin(content, text(item.title, 25, R.color.text_primary, true), 12);
        addInfoCard(content, "ABOUT", item.description);
        addInfoCard(content, "FILE INFORMATION", item.metadata);
        MaterialButton unavailable = fullButton("Download document", true);
        addTopMargin(content, unavailable, 14);
        unavailable.setOnClickListener(v -> {
            Validation.Result typeRes = Validation.validateFileType(item.title + ".pdf", new String[]{".pdf", ".doc", ".docx"});
            Validation.Result sizeRes = Validation.validateFileSize(1024 * 1024, 10 * 1024 * 1024);
            if (!typeRes.valid) {
                UiFeedback.showError(this, "Unsupported file format", typeRes.error, null);
                return;
            }
            if (!sizeRes.valid) {
                UiFeedback.showError(this, "File size limit exceeded", sizeRes.error, null);
                return;
            }
            UiFeedback.showError(
                    this,
                    "Offline preview unavailable",
                    "\"" + item.title + "\" (" + item.metadata + ") is listed in the local directory. File downloads will be available once the Spring Boot document service is connected.",
                    null
            );
        });
    }

    private String nextRequestAction(String status) {
        if ("Open".equals(status)) return "Schedule request";
        if ("Scheduled".equals(status)) return "Mark as Completed";
        return "Reopen request";
    }

    private String nextVisitorAction(String status) {
        if ("Expected".equals(status)) return "Approve visitor";
        if ("Approved".equals(status)) return "Record check-in";
        if ("Checked in".equals(status)) return "Record check-out";
        return "Reset to Expected";
    }

    // --- FORMS ---

    private void showComplaintForm(FrameLayout panel) {
        LinearLayout content = formContent(panel, "Report a maintenance issue. Your request will be saved locally.");
        Field title = createField("Issue title", "For example, lift door not closing", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        Field description = createField("Description", "Add details to help the team", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, true);
        description.input.setMinLines(3);
        Field unit = createField("Apartment / area", "Apartment or common area", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, false);
        unit.input.setText(safe(repository.getCurrentUser().apartmentUnit, "Common area"));
        addField(content, title);
        addField(content, description);
        Spinner category = addSpinner(content, "Category", new String[]{"Plumbing", "Electrical", "Elevator", "Facilities", "Cleaning", "Other"});
        Spinner priority = addSpinner(content, "Priority", new String[]{"Normal", "High", "Low"});
        addField(content, unit);
        for (Field f : new Field[]{title, description, unit}) setupClearErrorOnType(f);

        MaterialButton submit = fullButton("Submit complaint", true);
        addTopMargin(content, submit, 18);
        submit.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean valid = true;
            Validation.Result r;
            r = Validation.validateTitle(title.value(), "Issue title", 3, 100);
            if (!r.valid) { title.error(r.error); valid = false; } else title.clearError();
            r = Validation.validateDescription(description.value(), "Description", 8, 500);
            if (!r.valid) { description.error(r.error); valid = false; } else description.clearError();
            r = Validation.validateUnit(unit.value());
            if (!r.valid) { unit.error(r.error); valid = false; } else unit.clearError();
            r = Validation.validateSpinnerSelection(selected(category), "category");
            if (!r.valid) { Toast.makeText(this, r.error, Toast.LENGTH_SHORT).show(); valid = false; }
            if (!valid) {
                Motion.shakeError(submit);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(submit, "Submitting...",
                    title.input, description.input, unit.input, category, priority);
            hideKeyboard();
            try {
                repository.addComplaint(title.value(), description.value(),
                        selected(category), selected(priority), unit.value());
                Motion.completeButtonWithConfirmation(submit,
                        getString(R.string.feedback_complaint_submitted),
                        "Submit complaint",
                        () -> {
                            isSubmitting = false;
                            UiFeedback.showSuccess(
                                    this,
                                    getString(R.string.feedback_complaint_submitted),
                                    getString(R.string.feedback_complaint_submitted_subtitle),
                                    800L,
                                    () -> replaceCurrent("COMPLAINTS", "")
                            );
                        },
                        title.input, description.input, unit.input, category, priority);
            } catch (Exception ex) {
                isSubmitting = false;
                Motion.restoreButton(submit, "Submit complaint",
                        title.input, description.input, unit.input, category, priority);
                UiFeedback.showError(this, "Could not submit complaint",
                        safe(ex.getMessage(), "Please review the form and try again."),
                        submit::performClick);
            }
        });
    }

    private void showRequestForm(FrameLayout panel) {
        LinearLayout content = formContent(panel, "Send a request to the community team.");
        Field title = createField("Request title", "For example, reserve community room", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        Field description = createField("Description", "What do you need help with?", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, true);
        description.input.setMinLines(3);
        Field unit = createField("Apartment / unit", "Your apartment", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, false);
        unit.input.setText(safe(repository.getCurrentUser().apartmentUnit, ""));
        addField(content, title);
        addField(content, description);
        Spinner category = addSpinner(content, "Request category", new String[]{"Amenity booking", "Parcel assistance", "Move-in / move-out", "Common area", "Other"});
        addField(content, unit);
        for (Field f : new Field[]{title, description, unit}) setupClearErrorOnType(f);

        MaterialButton submit = fullButton("Submit service request", true);
        addTopMargin(content, submit, 16);
        submit.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean valid = true;
            Validation.Result r;
            r = Validation.validateTitle(title.value(), "Request title", 3, 100);
            if (!r.valid) { title.error(r.error); valid = false; } else title.clearError();
            r = Validation.validateDescription(description.value(), "Description", 8, 500);
            if (!r.valid) { description.error(r.error); valid = false; } else description.clearError();
            r = Validation.validateUnit(unit.value());
            if (!r.valid) { unit.error(r.error); valid = false; } else unit.clearError();
            if (!valid) {
                Motion.shakeError(submit);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(submit, "Saving...",
                    title.input, description.input, unit.input, category);
            hideKeyboard();
            try {
                repository.addRequest(title.value(), description.value(), selected(category), unit.value());
                Motion.completeButtonWithConfirmation(submit,
                        getString(R.string.feedback_request_saved),
                        "Submit service request",
                        () -> {
                            isSubmitting = false;
                            UiFeedback.showSuccess(
                                    this,
                                    getString(R.string.feedback_request_saved),
                                    getString(R.string.feedback_request_saved_subtitle),
                                    800L,
                                    () -> replaceCurrent("REQUESTS", "")
                            );
                        },
                        title.input, description.input, unit.input, category);
            } catch (Exception ex) {
                isSubmitting = false;
                Motion.restoreButton(submit, "Submit service request",
                        title.input, description.input, unit.input, category);
                UiFeedback.showError(this, "Could not save request",
                        safe(ex.getMessage(), "Please review the form and try again."),
                        submit::performClick);
            }
        });
    }

    private void showVisitorForm(FrameLayout panel) {
        LinearLayout content = formContent(panel, "Register an expected visitor. Details are saved locally.");
        Field name = createField("Visitor name", "Full name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        Field unit = createField("Host apartment / unit", "For example, C-204", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, false);
        unit.input.setText(safe(repository.getCurrentUser().apartmentUnit, ""));
        Field purpose = createField("Visit purpose", "Guest, delivery, service…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        Field phone = createField("Visitor phone (optional)", "Phone number", InputType.TYPE_CLASS_PHONE, false);
        Calendar arrival = Calendar.getInstance();
        arrival.add(Calendar.HOUR_OF_DAY, 1);
        TextView arrivalValue = text(formatArrival(arrival), 14, R.color.text_primary, true);
        MaterialButton chooseTime = fullButton("Change date and time", false);
        chooseTime.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
                arrival.set(Calendar.YEAR, year);
                arrival.set(Calendar.MONTH, month);
                arrival.set(Calendar.DAY_OF_MONTH, day);
                TimePickerDialog timePicker = new TimePickerDialog(this, (timeView, hour, minute) -> {
                    arrival.set(Calendar.HOUR_OF_DAY, hour);
                    arrival.set(Calendar.MINUTE, minute);
                    arrival.set(Calendar.SECOND, 0);
                    Validation.Result dateRes = Validation.validateDateNotPast(arrival.getTimeInMillis());
                    if (!dateRes.valid) {
                        Toast.makeText(this, dateRes.error, Toast.LENGTH_SHORT).show();
                        arrival.add(Calendar.HOUR_OF_DAY, 1);
                    }
                    arrivalValue.setText(formatArrival(arrival));
                }, arrival.get(Calendar.HOUR_OF_DAY), arrival.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(this));
                timePicker.show();
            }, arrival.get(Calendar.YEAR), arrival.get(Calendar.MONTH), arrival.get(Calendar.DAY_OF_MONTH));
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000L);
            datePicker.show();
        });
        addField(content, name);
        addField(content, unit);
        addField(content, purpose);
        addField(content, phone);
        addSectionHeader(content, "Expected arrival", null, null, 4);
        content.addView(arrivalValue);
        addTopMargin(content, chooseTime, 8);
        for (Field f : new Field[]{name, unit, purpose, phone}) setupClearErrorOnType(f);

        MaterialButton submit = fullButton("Save visitor", true);
        addTopMargin(content, submit, 18);
        submit.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean valid = true;
            Validation.Result r;
            r = Validation.validateVisitorName(name.value());
            if (!r.valid) { name.error(r.error); valid = false; } else name.clearError();
            r = Validation.validateUnit(unit.value());
            if (!r.valid) { unit.error(r.error); valid = false; } else unit.clearError();
            r = Validation.validatePurpose(purpose.value());
            if (!r.valid) { purpose.error(r.error); valid = false; } else purpose.clearError();
            r = Validation.validatePhone(phone.value(), false);
            if (!r.valid) { phone.error(r.error); valid = false; } else phone.clearError();
            r = Validation.validateDateNotPast(arrival.getTimeInMillis());
            if (!r.valid) { Toast.makeText(this, r.error, Toast.LENGTH_SHORT).show(); valid = false; }
            if (!valid) {
                Motion.shakeError(submit);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(submit, "Saving...",
                    name.input, unit.input, purpose.input, phone.input, chooseTime);
            hideKeyboard();
            try {
                repository.addVisitor(name.value(), unit.value(), purpose.value(),
                        formatArrival(arrival), phone.value());
                Motion.completeButtonWithConfirmation(submit,
                        getString(R.string.feedback_visitor_registered),
                        "Save visitor",
                        () -> {
                            isSubmitting = false;
                            UiFeedback.showSuccess(
                                    this,
                                    getString(R.string.feedback_visitor_registered),
                                    getString(R.string.feedback_visitor_registered_subtitle),
                                    800L,
                                    () -> replaceCurrent("VISITORS", "")
                            );
                        },
                        name.input, unit.input, purpose.input, phone.input, chooseTime);
            } catch (Exception ex) {
                isSubmitting = false;
                Motion.restoreButton(submit, "Save visitor",
                        name.input, unit.input, purpose.input, phone.input, chooseTime);
                UiFeedback.showError(this, "Could not register visitor",
                        safe(ex.getMessage(), "Please review the visitor details and try again."),
                        submit::performClick);
            }
        });
    }

    private String formatArrival(Calendar calendar) {
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(calendar.getTime());
        String time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(calendar.getTime());
        return date + " · " + time;
    }

    private void showAnnouncementForm(FrameLayout panel) {
        LinearLayout content = formContent(panel, "Publish an announcement for the community.");
        Field title = createField("Announcement title", "Clear, helpful title", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        Field description = createField("Announcement details", "Share update with residents", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, true);
        description.input.setMinLines(4);
        addField(content, title);
        addField(content, description);
        Spinner category = addSpinner(content, "Category", new String[]{"Facilities", "Community", "Events"});
        Spinner priority = addSpinner(content, "Priority", new String[]{"Normal", "Important"});
        for (Field f : new Field[]{title, description}) setupClearErrorOnType(f);

        MaterialButton publish = fullButton("Publish announcement", true);
        addTopMargin(content, publish, 14);
        publish.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean valid = true;
            Validation.Result r;
            r = Validation.validateTitle(title.value(), "Title", 3, 100);
            if (!r.valid) { title.error(r.error); valid = false; } else title.clearError();
            r = Validation.validateDescription(description.value(), "Details", 8, 1000);
            if (!r.valid) { description.error(r.error); valid = false; } else description.clearError();
            if (!valid) {
                Motion.shakeError(publish);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(publish, "Publishing...",
                    title.input, description.input, category, priority);
            hideKeyboard();
            repository.addAnnouncement(title.value(), description.value(), selected(category), selected(priority));
            Motion.completeButtonWithConfirmation(publish,
                    getString(R.string.feedback_announcement_published),
                    "Publish announcement",
                    () -> {
                        isSubmitting = false;
                        UiFeedback.showSuccess(
                                this,
                                getString(R.string.feedback_announcement_published),
                                "Your announcement is now visible to the community.",
                                800L,
                                () -> replaceCurrent("ANNOUNCEMENTS", "")
                        );
                    },
                    title.input, description.input, category, priority);
        });
    }

    private void showEventForm(FrameLayout panel) {
        LinearLayout content = formContent(panel, "Create a community event.");
        Field title = createField("Event title", "For example, coffee morning", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        Field description = createField("Description", "What should residents know?", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, true);
        description.input.setMinLines(3);
        Field location = createField("Location", "Community room, garden…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        Field dateTime = createField("Date and time", "Choose a future date and time", InputType.TYPE_CLASS_TEXT, false);
        dateTime.input.setFocusable(false);
        dateTime.input.setClickable(true);
        Calendar initialEventTime = Calendar.getInstance();
        initialEventTime.add(Calendar.DAY_OF_YEAR, 1);
        initialEventTime.set(Calendar.HOUR_OF_DAY, 10);
        initialEventTime.set(Calendar.MINUTE, 0);
        initialEventTime.set(Calendar.SECOND, 0);
        initialEventTime.set(Calendar.MILLISECOND, 0);
        final Calendar[] selectedEventTime = new Calendar[]{(Calendar) initialEventTime.clone()};
        dateTime.input.setText(new java.text.SimpleDateFormat("EEE, MMM d · hh:mm a", Locale.getDefault()).format(initialEventTime.getTime()));
        View.OnClickListener chooseEventDateTime = ignored -> {
            Calendar now = Calendar.getInstance();
            Calendar current = selectedEventTime[0] == null ? (Calendar) now.clone() : (Calendar) selectedEventTime[0].clone();
            DatePickerDialog datePicker = new DatePickerDialog(this, (picker, year, month, day) -> {
                Calendar chosenDate = Calendar.getInstance();
                chosenDate.set(year, month, day, 10, 0, 0);
                chosenDate.set(Calendar.MILLISECOND, 0);
                Calendar today = Calendar.getInstance();
                if (chosenDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                        && chosenDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                    chosenDate.set(Calendar.HOUR_OF_DAY, today.get(Calendar.HOUR_OF_DAY));
                    chosenDate.set(Calendar.MINUTE, today.get(Calendar.MINUTE));
                }
                TimePickerDialog timePicker = new TimePickerDialog(this, (timeDialog, hour, minute) -> {
                    chosenDate.set(Calendar.HOUR_OF_DAY, hour);
                    chosenDate.set(Calendar.MINUTE, minute);
                    chosenDate.set(Calendar.SECOND, 0);
                    chosenDate.set(Calendar.MILLISECOND, 0);
                    if (!chosenDate.after(Calendar.getInstance())) {
                        dateTime.error("Choose a date and time in the future.");
                        return;
                    }
                    selectedEventTime[0] = (Calendar) chosenDate.clone();
                    dateTime.clearError();
                    dateTime.input.setText(new java.text.SimpleDateFormat("EEE, MMM d · hh:mm a", Locale.getDefault()).format(chosenDate.getTime()));
                }, chosenDate.get(Calendar.HOUR_OF_DAY), chosenDate.get(Calendar.MINUTE), false);
                timePicker.show();
            }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        };
        dateTime.input.setOnClickListener(chooseEventDateTime);
        dateTime.layout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        dateTime.layout.setEndIconDrawable(R.drawable.ic_calendar);
        dateTime.layout.setEndIconContentDescription("Choose event date and time");
        dateTime.layout.setEndIconOnClickListener(chooseEventDateTime);
        addField(content, title);
        addField(content, description);
        addField(content, location);
        addField(content, dateTime);
        for (Field f : new Field[]{title, description, location, dateTime}) setupClearErrorOnType(f);

        MaterialButton create = fullButton("Create event", true);
        addTopMargin(content, create, 14);
        create.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean valid = true;
            Validation.Result r;
            r = Validation.validateTitle(title.value(), "Event title", 3, 100);
            if (!r.valid) { title.error(r.error); valid = false; } else title.clearError();
            r = Validation.validateDescription(description.value(), "Description", 8, 500);
            if (!r.valid) { description.error(r.error); valid = false; } else description.clearError();
            r = Validation.validateTitle(location.value(), "Location", 2, 100);
            if (!r.valid) { location.error(r.error); valid = false; } else location.clearError();
            if (selectedEventTime[0] == null || !selectedEventTime[0].after(Calendar.getInstance())) {
                dateTime.error("Choose a date and time in the future.");
                valid = false;
            } else {
                dateTime.clearError();
            }
            if (!valid) {
                Motion.shakeError(create);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(create, "Creating...",
                    title.input, description.input, location.input, dateTime.input);
            hideKeyboard();
            repository.addEvent(title.value(), description.value(), location.value(), dateTime.value());
            Motion.completeButtonWithConfirmation(create,
                    getString(R.string.feedback_event_created),
                    "Create event",
                    () -> {
                        isSubmitting = false;
                        UiFeedback.showSuccess(
                                this,
                                getString(R.string.feedback_event_created),
                                "The community event has been added to the schedule.",
                                800L,
                                () -> replaceCurrent("EVENTS", "")
                        );
                    },
                    title.input, description.input, location.input, dateTime.input);
        });
    }

    private void showResidentForm(FrameLayout panel) {
        LinearLayout content = formContent(panel, "Add a resident to the directory. Unit must be unique.");
        Field name = createField("Resident name", "Full name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        Field unit = createField("Apartment / unit", "For example, A-204", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, false);
        Field household = createField("Household summary", "For example, 2 residents", InputType.TYPE_CLASS_TEXT, false);
        household.input.setText("1 resident");
        addField(content, name);
        addField(content, unit);
        addField(content, household);
        for (Field f : new Field[]{name, unit, household}) setupClearErrorOnType(f);

        MaterialButton save = fullButton("Add resident", true);
        addTopMargin(content, save, 14);
        save.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean valid = true;
            Validation.Result r;
            r = Validation.validateName(name.value());
            if (!r.valid) { name.error(r.error); valid = false; } else name.clearError();
            r = Validation.validateUnit(unit.value());
            if (!r.valid) { unit.error(r.error); valid = false; } else unit.clearError();
            r = Validation.validateTitle(household.value(), "Household", 2, 50);
            if (!r.valid) { household.error(r.error); valid = false; } else household.clearError();
            if (!valid) {
                Motion.shakeError(save);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(save, "Saving...", name.input, unit.input, household.input);
            hideKeyboard();
            try {
                repository.addResident(name.value(), unit.value(), safe(household.value(), "Resident"));
                Motion.completeButtonWithConfirmation(save,
                        getString(R.string.feedback_resident_added),
                        "Add resident",
                        () -> {
                            isSubmitting = false;
                            UiFeedback.showSuccess(
                                    this,
                                    getString(R.string.feedback_resident_added),
                                    name.value() + " (" + unit.value() + ") has been added to the directory.",
                                    800L,
                                    () -> replaceCurrent("RESIDENTS", "")
                            );
                        },
                        name.input, unit.input, household.input);
            } catch (IllegalArgumentException ex) {
                isSubmitting = false;
                Motion.restoreButton(save, "Add resident", name.input, unit.input, household.input);
                unit.error(ex.getMessage());
                Motion.shakeError(unit.layout);
                UiFeedback.showError(this, "Could not add resident", ex.getMessage(), () -> unit.input.requestFocus());
            }
        });
    }

    private void showProfileForm(FrameLayout panel) {
        LinearLayout content = formContent(panel, "Edit your profile information.");
        User user = repository.getCurrentUser();
        Field name = createField("Full name", "Your name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        Field email = createField("Email address", "name@example.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
        Field unit = createField("Apartment / unit", "For example, C-204", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, false);
        Field phone = createField("Phone (optional)", "Your phone number", InputType.TYPE_CLASS_PHONE, false);
        name.input.setText(user.name);
        email.input.setText(user.email);
        unit.input.setText(user.apartmentUnit);
        phone.input.setText(safe(user.phone, ""));
        addField(content, name);
        addField(content, email);
        addField(content, unit);
        addField(content, phone);
        for (Field f : new Field[]{name, email, unit, phone}) setupClearErrorOnType(f);

        MaterialButton save = fullButton("Save profile", true);
        addTopMargin(content, save, 14);
        save.setOnClickListener(v -> {
            if (isSubmitting) return;
            boolean valid = true;
            Validation.Result r;
            r = Validation.validateName(name.value());
            if (!r.valid) { name.error(r.error); valid = false; } else name.clearError();
            r = Validation.validateEmail(email.value());
            if (!r.valid) { email.error(r.error); valid = false; } else email.clearError();
            r = Validation.validateUnit(unit.value());
            if (!r.valid) { unit.error(r.error); valid = false; } else unit.clearError();
            r = Validation.validatePhone(phone.value(), false);
            if (!r.valid) { phone.error(r.error); valid = false; } else phone.clearError();
            if (!valid) {
                Motion.shakeError(save);
                return;
            }
            isSubmitting = true;
            Motion.setButtonLoading(save, "Saving...", name.input, email.input, unit.input, phone.input);
            hideKeyboard();
            String profileError = repository.updateProfile(
                    name.value(), email.value(), unit.value(), phone.value());
            if (profileError != null) {
                isSubmitting = false;
                Motion.restoreButton(save, "Save profile",
                        name.input, email.input, unit.input, phone.input);
                boolean emailConflict = repository.isEmailRegistered(email.value())
                        && !email.value().equalsIgnoreCase(repository.getCurrentUser().email);
                if (emailConflict) {
                    email.error(profileError);
                    Motion.shakeError(email.layout);
                } else {
                    UiFeedback.showError(this, "Could not save profile", profileError, null);
                }
                return;
            }
            Motion.completeButtonWithConfirmation(save,
                    getString(R.string.feedback_profile_updated),
                    "Save profile",
                    () -> {
                        isSubmitting = false;
                        UiFeedback.showSuccess(
                                this,
                                getString(R.string.feedback_profile_updated),
                                "Your profile details have been saved.",
                                750L,
                                () -> replaceCurrent("PROFILE", "")
                        );
                    },
                    name.input, email.input, unit.input, phone.input);
        });
    }

    private LinearLayout formContent(FrameLayout panel, String intro) {
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout content = verticalContent();
        content.addView(text(intro, 13, R.color.text_secondary, false));
        scroll.addView(content);
        panel.addView(scroll);
        return content;
    }

    private void addField(LinearLayout parent, Field field) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        parent.addView(field.layout, params);
    }

    private void setupClearErrorOnType(Field field) {
        field.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (field.layout.getError() != null) field.clearError();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private Spinner addSpinner(LinearLayout parent, String label, String[] values) {
        TextView labelView = text(label, 12, R.color.text_secondary, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(12);
        parent.addView(labelView, labelParams);
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        params.topMargin = dp(4);
        parent.addView(spinner, params);
        return spinner;
    }

    private String selected(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        return item == null ? "" : item.toString();
    }

    private boolean canAdd(String route) {
        CommunityRole role = repository.getCurrentUser().role;
        if ("ANNOUNCEMENTS".equals(route) || "EVENTS".equals(route) || "RESIDENTS".equals(route)) return role == CommunityRole.MANAGEMENT;
        if ("COMPLAINTS".equals(route) || "REQUESTS".equals(route) || "VISITORS".equals(route)) return role == CommunityRole.RESIDENT || role == CommunityRole.MANAGEMENT;
        return false;
    }

    private void onAdd(String route) {
        if (!canAdd(route)) return;
        switch (route) {
            case "COMPLAINTS": navigate("FORM_COMPLAINT"); break;
            case "REQUESTS": navigate("FORM_REQUEST"); break;
            case "VISITORS": navigate("FORM_VISITOR"); break;
            case "ANNOUNCEMENTS": navigate("FORM_ANNOUNCEMENT"); break;
            case "EVENTS": navigate("FORM_EVENT"); break;
            case "RESIDENTS": navigate("FORM_RESIDENT"); break;
        }
    }

    private void addActionGrid(LinearLayout parent, List<Tile> tiles) {
        List<View> revealed = new ArrayList<>();
        for (int i = 0; i < tiles.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Tile first = tiles.get(i);
            View firstTile = actionTile(first);
            revealed.add(firstTile);
            row.addView(firstTile, new LinearLayout.LayoutParams(0, dp(132), 1f));
            if (i + 1 < tiles.size()) {
                LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, dp(132), 1f);
                secondParams.leftMargin = dp(10);
                View secondTile = actionTile(tiles.get(i + 1));
                revealed.add(secondTile);
                row.addView(secondTile, secondParams);
            } else {
                View spacer = new View(this);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, dp(132), 1f);
                spacerParams.leftMargin = dp(10);
                row.addView(spacer, spacerParams);
            }
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dp(9);
            parent.addView(row, rowParams);
        }
        if (animateCurrentRender) Motion.staggerEnter(revealed);
    }

    private View actionTile(Tile tile) {
        View root = LayoutInflater.from(this).inflate(R.layout.view_action_tile, null, false);
        MaterialCardView card = root.findViewById(R.id.actionTileCard);
        ImageView icon = root.findViewById(R.id.actionIcon);
        icon.setImageResource(tile.icon);
        icon.setColorFilter(color(R.color.primary));
        ((TextView) root.findViewById(R.id.actionTitle)).setText(tile.title);
        ((TextView) root.findViewById(R.id.actionCaption)).setText(tile.caption);
        card.setContentDescription(tile.title + ". " + tile.caption);
        card.setOnClickListener(v -> {
            if (tile.destination.startsWith("OPS_")) openOperations(tile.destination, "");
            else navigate(tile.destination);
        });
        Motion.attachPressFeedback(card);
        return root;
    }

    private MaterialCardView compactInfoCard(String title, String subtitle, String badgeText) {
        MaterialCardView card = card();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(copy, copyParams);
        copy.addView(text(title, 14, R.color.text_primary, true));
        TextView sub = text(subtitle, 11, R.color.text_secondary, false);
        addTopMargin(copy, sub, 4);
        TextView badge = text(badgeText, 11, R.color.primary, true);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badge.setBackground(shape(color(R.color.primary_light), dp(20), color(R.color.primary_light), 0));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeParams.leftMargin = dp(8);
        row.addView(badge, badgeParams);
        card.addView(row);
        return card;
    }

    private void addSectionHeader(LinearLayout parent, String title, String action, Runnable onAction, int topMargin) {
        View root = LayoutInflater.from(this).inflate(R.layout.view_section_header, parent, false);
        TextView heading = root.findViewById(R.id.sectionTitle);
        TextView actionView = root.findViewById(R.id.sectionAction);
        heading.setText(title);
        if (action != null) {
            actionView.setText(action);
            actionView.setVisibility(View.VISIBLE);
            actionView.setOnClickListener(v -> { if (onAction != null) onAction.run(); });
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMargin);
        parent.addView(root, params);
    }

    private void addInfoCard(LinearLayout parent, String heading, String body) {
        View root = LayoutInflater.from(this).inflate(R.layout.view_info_card, parent, false);
        MaterialCardView card = root.findViewById(R.id.infoCard);
        ((TextView) root.findViewById(R.id.infoHeading)).setText(heading);
        ((TextView) root.findViewById(R.id.infoBody)).setText(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        parent.addView(card, params);
    }

    private void addPill(LinearLayout parent, String label) {
        TextView pill = text(label, 11, R.color.primary, true);
        pill.setPadding(dp(11), dp(7), dp(11), dp(7));
        pill.setBackground(shape(color(R.color.primary_light), dp(22), color(R.color.primary_light), 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        parent.addView(pill, params);
    }

    private void addTimelineRow(LinearLayout parent, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        dot.setBackground(shape(color(R.color.primary), dp(8), color(R.color.primary), 0));
        row.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));
        TextView label = text(value, 13, R.color.text_secondary, false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.leftMargin = dp(10);
        row.addView(label, labelParams);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(9);
        parent.addView(row, rowParams);
    }

    private void addKeyValue(LinearLayout parent, String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(key, 11, R.color.text_muted, true);
        row.addView(label);
        TextView content = text(value, 14, R.color.text_primary, false);
        addTopMargin(row, content, 3);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(14);
        parent.addView(row, params);
    }

    private void addNotFound(LinearLayout content) {
        addInfoCard(content, "ITEM UNAVAILABLE",
                "This record is no longer available or has been removed. Return to the list and choose another entry.");
        MaterialButton backBtn = fullButton("Return to previous screen", false);
        addTopMargin(content, backBtn, 14);
        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private void confirmLogout() {
        UiFeedback.showLogoutConfirm(this, () -> {
            repository.logout();
            rootRoute("LOGIN");
        });
    }

    private void openDialer(String number) {
        if (!isDialable(number)) {
            UiFeedback.showError(this, "Invalid phone number", "No valid phone number is configured for this contact.", null);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number.trim(), null));
        try { startActivity(intent); }
        catch (Exception exception) {
            UiFeedback.showError(this, "Dialer unavailable", "No phone dialer application is available on this device.", null);
        }
    }

    private boolean isDialable(String number) {
        if (number == null || number.trim().isEmpty()) return false;
        int digits = 0;
        String allowedSeparators = " ()-./";
        for (int i = 0; i < number.length(); i++) {
            char character = number.charAt(i);
            if (Character.isDigit(character)) digits++;
            else if (character == '+' && i == 0) { }
            else if (allowedSeparators.indexOf(character) < 0) return false;
        }
        return digits >= 3 && digits <= 15;
    }

    private MaterialButton fullButton(String label, boolean primary) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setCornerRadius(dp(12));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(dp(52));
        button.setAllCaps(false);
        button.setBackgroundTintList(ColorStateList.valueOf(color(primary ? R.color.primary : R.color.surface)));
        button.setTextColor(color(primary ? R.color.white : R.color.primary));
        if (!primary) {
            button.setStrokeWidth(dp(1));
            button.setStrokeColor(ColorStateList.valueOf(color(R.color.outline)));
        }
        button.setPadding(dp(16), dp(10), dp(16), dp(10));
        button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Motion.attachPressFeedback(button);
        return button;
    }

    private MaterialButton smallButton(String label, boolean primary) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setCornerRadius(dp(10));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(dp(40));
        button.setAllCaps(false);
        button.setBackgroundTintList(ColorStateList.valueOf(color(primary ? R.color.primary : R.color.surface)));
        button.setTextColor(color(primary ? R.color.white : R.color.primary));
        if (!primary) {
            button.setStrokeWidth(dp(1));
            button.setStrokeColor(ColorStateList.valueOf(color(R.color.outline)));
        }
        Motion.attachPressFeedback(button);
        return button;
    }

    private MaterialButton textButton(String label) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(color(R.color.primary));
        button.setAllCaps(false);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    private Field createField(String label, String hint, int inputType, boolean multiline) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxBackgroundColor(color(R.color.surface));
        layout.setHint(label == null || label.isEmpty() ? null : label);
        layout.setBoxCornerRadii(dp(12), dp(12), dp(12), dp(12));
        layout.setBoxStrokeWidth(dp(1));
        layout.setBoxStrokeWidthFocused(dp(2));
        int[][] strokeStates = new int[][]{
                {android.R.attr.state_focused, android.R.attr.state_enabled},
                {android.R.attr.state_enabled},
                {}};
        layout.setBoxStrokeColorStateList(new ColorStateList(strokeStates, new int[]{
                color(R.color.primary), color(R.color.outline_strong), color(R.color.outline_strong)}));
        layout.setErrorTextColor(ColorStateList.valueOf(color(R.color.danger)));
        layout.setHintTextColor(ColorStateList.valueOf(color(R.color.text_secondary)));
        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setTextSize(16);
        input.setTextColor(color(R.color.text_primary));
        input.setHintTextColor(color(R.color.text_muted));
        input.setInputType(inputType);
        if (hint != null && !hint.isEmpty()) layout.setPlaceholderText(hint);
        if (!multiline) input.setSingleLine(true);
        else input.setGravity(Gravity.TOP | Gravity.START);
        layout.addView(input, new TextInputLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new Field(layout, input);
    }

    private ImageButton iconButton(@DrawableRes int icon, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setColorFilter(color(R.color.text_primary));
        button.setContentDescription(description);
        button.setScaleType(ImageView.ScaleType.CENTER);
        android.util.TypedValue selectable = new android.util.TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, selectable, true)
                && selectable.resourceId != 0) {
            button.setBackgroundResource(selectable.resourceId);
        } else {
            button.setBackgroundColor(Color.TRANSPARENT);
        }
        return button;
    }

    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(color(R.color.surface));
        card.setStrokeColor(color(R.color.outline));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        return card;
    }

    private LinearLayout verticalContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(10), dp(20), dp(26));
        return content;
    }

    private TextView text(String value, int sizeSp, int colorResource, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value == null ? "" : value);
        view.setTextSize(sizeSp);
        view.setTextColor(color(colorResource));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private GradientDrawable shape(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(radius);
        if (strokeWidth > 0) background.setStroke(strokeWidth, stroke);
        return background;
    }

    private void addTopMargin(ViewGroup parent, View child, int marginDp) {
        ViewGroup.LayoutParams existing = child.getLayoutParams();
        LinearLayout.LayoutParams params;
        if (existing instanceof LinearLayout.LayoutParams) {
            params = (LinearLayout.LayoutParams) existing;
        } else if (existing != null) {
            params = new LinearLayout.LayoutParams(existing);
        } else {
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.topMargin = dp(marginDp);
        if (child.getParent() == null) {
            parent.addView(child, params);
        } else if (child.getParent() == parent) {
            child.setLayoutParams(params);
        }
    }

    private int color(int resource) { return ContextCompat.getColor(this, resource); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String snip(String value, int length) {
        if (value == null || value.length() <= length) return value == null ? "" : value;
        return value.substring(0, length).trim() + "…";
    }

    private String initials(String value) {
        if (value == null || value.trim().isEmpty()) return "CG";
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.getDefault());
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.getDefault());
    }

    private List<Complaint> visibleComplaints() {
        if (repository.isIssueOperationsAvailable()) {
            com.apartment.community.operations.OperationResult<List<Complaint>> result =
                    repository.getOperationalIssues();
            if (result.ok) return result.value;
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }
        return repository.getComplaints();
    }

    private Complaint findVisibleComplaint(String id) {
        for (Complaint item : visibleComplaints()) if (item.id.equals(id)) return item;
        return null;
    }

    private void openOperations(String section, String recordId) {
        Intent intent = OperationsActivity.createIntent(this, section, recordId);
        startActivityForResult(intent, REQUEST_OPERATIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPERATIONS && resultCode == RESULT_OK) renderQuietly();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View focus = getCurrentFocus();
            if (manager != null && focus != null) manager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }
}
