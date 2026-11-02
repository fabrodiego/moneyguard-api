package com.fabrodiego.moneyguard.discord;

/** O usuario do Discord ainda nao usou /vincular. */
public class DiscordNotLinkedException extends RuntimeException {

    public DiscordNotLinkedException() {
        super("Conta do Discord nao vinculada");
    }
}
