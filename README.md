# Kaveri Login CLI

This Java command-line program opens Kaveri Online Services in Chrome. Sign in manually with your username, password, CAPTCHA, and OTP in Chrome. Once the dashboard appears, press Enter in the terminal to continue the EC search automation.

## Prerequisites

- JDK 21 or later
- Maven 3.9 or later
- Google Chrome

## Run

```sh
mvn compile exec:java
```

The program opens `https://kaveri.karnataka.gov.in/`. Complete the entire login flow in Chrome. When the dashboard is visible, return to the terminal and press Enter to begin the automated EC search steps.

## Selector Overrides

The public landing page does not expose the login fields, so the program tries common CSS selectors. When the portal uses different selectors, supply the actual CSS selector as a JVM property:

```sh
mvn compile exec:java \
  -Dkaveri.startApplicationSelector='#actual-start-application-button' \
  -Dkaveri.onlineEcSelector='#actual-online-ec-option' \
  -Dkaveri.ecPrerequisiteContinueSelector='#actual-continue-button' \
  -Dkaveri.after2004Selector='#actual-after-2004-option' \
  -Dkaveri.proceedSelector='#actual-proceed-button' \
  -Dkaveri.searchByPropertyNameSelector='#actual-property-name-tab' \
  -Dkaveri.districtSelector='#actual-district-select' \
  -Dkaveri.talukaSelector='#actual-taluka-select' \
  -Dkaveri.hobliSelector='#actual-hobli-select' \
  -Dkaveri.villageSelector='#actual-village-select' \
  -Dkaveri.propertyNameSelector='#actual-property-name-input' \
  -Dkaveri.propertySearchSelector='#actual-property-search-button'
```

After OTP verification, the program selects the dashboard's `START A NEW APPLICATION` button, the `ENCUMBERANCE CERTIFICATE -(ONLINE EC)` service, then `Continue`, `After 01/01/2004`, and `Proceed`. It then selects `Search by Property Name`, chooses Tumkur (or Tumakuru), and traverses every available Taluka, Hobli/Town, and Village. At each location, it enters each supplied English and Kannada name spelling separately. The portal does not provide client-side regex search, so the script cannot submit a regex; it uses the individual equivalent variants instead. It does not extract, save, or export results.

The CLI requires confirmation before the location-wide traversal begins, because the portal may have many locations and each name search is submitted separately. It uses a bounded four-worker pool to prepare name-search jobs concurrently, while one browser session submits portal actions in order. Selenium drivers are not thread-safe, and parallel browser sessions would require separate portal logins, so browser requests intentionally remain serialized. The browser remains visible so you can review the result.