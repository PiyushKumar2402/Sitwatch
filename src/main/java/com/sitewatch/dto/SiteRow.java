package com.sitewatch.dto;

import com.sitewatch.model.Website;
import lombok.Value;

import java.util.List;

/** One dashboard row: the site, its stats, and its last checks as "u"/"d" markers for the heartbeat strip. */
@Value
public class SiteRow {
    Website site;
    SiteStats stats;
    List<String> strip;
}
