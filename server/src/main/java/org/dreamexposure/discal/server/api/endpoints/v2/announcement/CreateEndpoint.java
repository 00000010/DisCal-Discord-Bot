package org.dreamexposure.discal.server.api.endpoints.v2.announcement;

import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.enums.announcement.AnnouncementType;
import org.dreamexposure.discal.core.enums.event.EventColor;
import org.dreamexposure.discal.core.logger.Logger;
import org.dreamexposure.discal.core.object.announcement.Announcement;
import org.dreamexposure.discal.core.object.web.AuthenticationState;
import org.dreamexposure.discal.core.utils.JsonUtils;
import org.dreamexposure.discal.server.utils.Authentication;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import discord4j.core.object.util.Snowflake;

@RestController
@RequestMapping("/v2/announcement")
public class CreateEndpoint {
	@PostMapping(value = "/create", produces = "application/json")
	public String createAnnouncement(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
		//Authenticate...
		AuthenticationState authState = Authentication.authenticate(request);
		if (!authState.isSuccess()) {
			response.setStatus(authState.getStatus());
			response.setContentType("application/json");
			return authState.toJson();
		}

		//Okay, now handle actual request.
		try {
			JSONObject body = new JSONObject(requestBody);
			Snowflake guildId = Snowflake.of(body.getLong("guild_id"));

			Announcement a = new Announcement(guildId);

			a.setAnnouncementChannelId(body.getString("channel"));
			a.setAnnouncementType(AnnouncementType.fromValue(body.getString("type")));

			if (a.getAnnouncementType().equals(AnnouncementType.COLOR))
				a.setEventColor(EventColor.fromNameOrHexOrID(body.getString("color")));

			if (a.getAnnouncementType().equals(AnnouncementType.RECUR) ||
					a.getAnnouncementType().equals(AnnouncementType.SPECIFIC))
				a.setEventId(body.getString("event_id"));

			a.setHoursBefore(body.getInt("hours"));
			a.setMinutesBefore(body.getInt("minutes"));

			if (body.has("info"))
				a.setInfo(body.getString("info"));
			else
				a.setInfo("N/a");

			if (body.has("info_only"))
				a.setInfoOnly(body.getBoolean("info_only"));

			if (DatabaseManager.getManager().updateAnnouncement(a)) {
				JSONObject responseBody = new JSONObject();
				responseBody.put("message", "Announcement successfully created");
				responseBody.put("announcement_id", a.getAnnouncementId().toString());

				response.setContentType("application/json");
				response.setStatus(200);
				return responseBody.toString();
			}

			response.setContentType("application/json");
			response.setStatus(500);
			return JsonUtils.getJsonResponseMessage("Internal Server Error");
		} catch (JSONException e) {
			e.printStackTrace();

			response.setContentType("application/json");
			response.setStatus(400);
			return JsonUtils.getJsonResponseMessage("Bad Request");
		} catch (Exception e) {
			Logger.getLogger().exception(null, "[API-v2] Internal create announcement error", e, true, this.getClass());

			response.setContentType("application/json");
			response.setStatus(500);
			return JsonUtils.getJsonResponseMessage("Internal Server Error");
		}
	}
}