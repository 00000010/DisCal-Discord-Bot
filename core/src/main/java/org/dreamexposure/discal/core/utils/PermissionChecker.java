package org.dreamexposure.discal.core.utils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Permission;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.logger.Logger;
import org.dreamexposure.discal.core.object.GuildSettings;

/**
 * Created by Nova Fox on 1/19/17.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
@SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
public class PermissionChecker {
	/**
	 * Checks if the user who sent the received message has the proper role to use a command.
	 *
	 * @param event The Event received to check for the user and guild.
	 * @return <code>true</code> if the user has the proper role, otherwise <code>false</code>.
	 */
	public static boolean hasSufficientRole(MessageCreateEvent event) {
		//TODO: Figure out exactly what is causing a NPE here...
		try {
			GuildSettings settings = DatabaseManager.getManager().getSettings(event.getGuild().block().getId());
			if (!settings.getControlRole().equalsIgnoreCase("everyone")) {
				User sender = event.getMessage().getAuthor().get();
				String roleId = settings.getControlRole();
				Role role = null;

				for (Role r : event.getMessage().getGuild().block().getRoles().toIterable()) {
					if (r.getId().asString().equals(roleId)) {
						role = r;
						break;
					}
				}

				if (role != null) {
					for (Role r : event.getGuild().block().getMemberById(sender.getId()).block().getRoles().toIterable()) {
						if (r.getId().asString().equals(role.getId().asString()) || r.getPosition().block() > role.getPosition().block())
							return true;

					}
					return false;
				} else {
					//Role not found... reset Db...
					settings.setControlRole("everyone");
					DatabaseManager.getManager().updateSettings(settings);
					return true;
				}
			}
		} catch (Exception e) {
			//Something broke so we will harmlessly allow access and alert the dev.
			Logger.getLogger().exception(event.getMessage().getAuthor().get(), "Failed to check for sufficient control role.", e, true, PermissionChecker.class);
			return true;
		}
		return true;
	}

	public static boolean hasSufficientRole(Guild guild, User user) {
		//TODO: Figure out exactly what is causing a NPE here...
		try {
			GuildSettings settings = DatabaseManager.getManager().getSettings(guild.getId());
			if (!settings.getControlRole().equalsIgnoreCase("everyone")) {
				String roleId = settings.getControlRole();
				Role role = null;

				for (Role r : guild.getRoles().toIterable()) {
					if (r.getId().asString().equals(roleId)) {
						role = r;
						break;
					}
				}

				if (role != null) {
					for (Role r : guild.getMemberById(user.getId()).block().getRoles().toIterable()) {
						if (r.getId().equals(role.getId()) || r.getPosition().block() > role.getPosition().block())
							return true;

					}
					return false;
				} else {
					//Role not found... reset Db...
					settings.setControlRole("everyone");
					DatabaseManager.getManager().updateSettings(settings);
					return true;
				}
			}
		} catch (Exception e) {
			//Something broke so we will harmlessly allow access and notify the dev team
			Logger.getLogger().exception(user, "Failed to check for sufficient control role.", e, true, PermissionChecker.class);
			return true;
		}
		return true;
	}

	public static boolean hasManageServerRole(MessageCreateEvent event) {
		return event.getMessage().getAuthor().get().asMember(event.getGuildId().get()).block().getBasePermissions().block().contains(Permission.MANAGE_GUILD);
	}

	public static boolean hasManageServerRole(Member m) {
		return m.getBasePermissions().block().contains(Permission.MANAGE_GUILD);
	}

	/**
	 * Checks if the user sent the command in a DisCal channel (if set).
	 *
	 * @param event The event received to check for the correct channel.
	 * @return <code>true</code> if in correct channel, otherwise <code>false</code>.
	 */
	public static boolean isCorrectChannel(MessageCreateEvent event) {
		try {
			GuildSettings settings = DatabaseManager.getManager().getSettings(event.getGuild().block().getId());
			if (settings.getDiscalChannel().equalsIgnoreCase("all"))
				return true;


			GuildChannel channel = null;
			for (GuildChannel c : event.getMessage().getGuild().block().getChannels().toIterable()) {
				if (c.getId().asString().equals(settings.getDiscalChannel())) {
					channel = c;
					break;
				}
			}

			if (channel != null)
				return event.getMessage().getChannel().block().getId().equals(channel.getId());


			//If we got here, the channel no longer exists, reset data and return true.
			settings.setDiscalChannel("all");
			DatabaseManager.getManager().updateSettings(settings);
			return true;
		} catch (Exception e) {
			//Catch any errors so that the bot always responds...
			Logger.getLogger().exception(event.getMember().get(), "Failed to check for discal channel.", e, true, PermissionChecker.class);
			return true;
		}
	}

	public static boolean botHasMessageManagePerms(MessageCreateEvent event) {
		return event.getGuild().flatMap(guild -> guild.getMemberById(event.getClient().getSelfId().get())).block().getBasePermissions().block().contains(Permission.MANAGE_MESSAGES);
	}
}
