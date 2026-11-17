package com.gurusainathp.movieed.data.remote.dto;

public class WatchProviderItem implements Comparable<WatchProviderItem> {

    private final TmdbProviderDto provider;
    private boolean isFlatrate;
    private boolean isRent;
    private boolean isBuy;

    public WatchProviderItem(TmdbProviderDto provider) {
        this.provider = provider;
    }

    public TmdbProviderDto getProvider() { return provider; }

    public boolean isFlatrate() { return isFlatrate; }
    public void setFlatrate(boolean flatrate) { isFlatrate = flatrate; }

    public boolean isRent() { return isRent; }
    public void setRent(boolean rent) { isRent = rent; }

    public boolean isBuy() { return isBuy; }
    public void setBuy(boolean buy) { isBuy = buy; }

    public String getBadgeText() {
        if (isFlatrate) {
            return null; // Available / Stream subscription has no badge
        }
        if (isBuy && isRent) {
            return "BUY/RENT";
        }
        if (isBuy) {
            return "BUY";
        }
        if (isRent) {
            return "RENT";
        }
        return null;
    }

    public int getSortPriority() {
        if (isFlatrate) return 0; // Subscription first
        if (isRent) return 1;     // Rent second
        if (isBuy) return 2;      // Buy third
        return 3;
    }

    @Override
    public int compareTo(WatchProviderItem other) {
        return Integer.compare(this.getSortPriority(), other.getSortPriority());
    }
}
