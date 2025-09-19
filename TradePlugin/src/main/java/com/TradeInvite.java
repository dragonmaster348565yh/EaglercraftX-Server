package com.example.tradeplugin;

import org.bukkit.entity.Player;

public class TradeInvite {
    private Player sender;
    private Player target;
    
    public TradeInvite(Player sender, Player target) {
        this.sender = sender;
        this.target = target;
    }
    
    public Player getSender() {
        return sender;
    }
    
    public Player getTarget() {
        return target;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TradeInvite that = (TradeInvite) obj;
        return sender.equals(that.sender) && target.equals(that.target);
    }
}