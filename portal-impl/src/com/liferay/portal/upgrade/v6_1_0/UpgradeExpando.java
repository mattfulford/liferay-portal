/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.upgrade.v6_1_0;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Terry Jia
 * @author Brian Wing Shun Chan
 */
public class UpgradeExpando extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		updateDefaultData();
		updateTypeSettingsIndexable();
		updateTypeSettingsSelection();
	}

	protected void updateDefaultData() throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection();

			ps = con.prepareStatement(
				"select columnId, defaultData from ExpandoColumn " +
					"where type_ = " + ExpandoColumnConstants.STRING_ARRAY);

			rs = ps.executeQuery();

			while (rs.next()) {
				long columnId = rs.getLong("columnId");
				String defaultData = rs.getString("defaultData");

				defaultData = StringUtil.replace(defaultData, "\\", "\\\\");

				updateDefaultData(columnId, defaultData);

				updateExpandoValue(columnId);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected void updateDefaultData(long columnId, String defaultData)
		throws Exception {

		Connection con = null;
		PreparedStatement ps = null;

		try {
			con = DataAccess.getConnection();

			ps = con.prepareStatement(
				"update ExpandoColumn set defaultData = ? where columnId = ?");

			ps.setString(1, defaultData);
			ps.setLong(2, columnId);

			ps.executeUpdate();
		}
		finally {
			DataAccess.cleanUp(con, ps);
		}
	}

	protected void updateExpandoValue(long columnId) throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection();

			ps = con.prepareStatement(
				"select data_, valueId from ExpandoValue where columnId = "
					+ columnId);

			rs = ps.executeQuery();

			while (rs.next()) {
				long valueId = rs.getLong("valueId");
				String data = rs.getString("data_");

				data = StringUtil.replace(data, "\\", "\\\\");

				updateExpandoValue(valueId, data);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected void updateExpandoValue(long valueId, String data)
		throws Exception {

		Connection con = null;
		PreparedStatement ps = null;

		try {
			con = DataAccess.getConnection();

			ps = con.prepareStatement(
				"update ExpandoValue set data_ = ? where valueId = ?");

			ps.setString(1, data);
			ps.setLong(2, valueId);

			ps.executeUpdate();
		}
		finally {
			DataAccess.cleanUp(con, ps);
		}
	}

	protected void updateTypeSettings(long columnId, String typeSettings)
		throws Exception {

		Connection con = null;
		PreparedStatement ps = null;

		try {
			con = DataAccess.getConnection();

			ps = con.prepareStatement(
				"update ExpandoColumn set typeSettings = ? where columnId = ?");

			ps.setString(1, typeSettings);
			ps.setLong(2, columnId);

			ps.executeUpdate();
		}
		finally {
			DataAccess.cleanUp(con, ps);
		}
	}

	protected void updateTypeSettingsIndexable() throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection();

			ps = con.prepareStatement(
				"select columnId, type_, typeSettings from ExpandoColumn " +
					"where typeSettings like '%indexable%'");

			rs = ps.executeQuery();

			while (rs.next()) {
				long columnId = rs.getLong("columnId");
				int type = rs.getInt("type_");
				String typeSettings = rs.getString("typeSettings");

				UnicodeProperties typeSettingsProperties =
					new UnicodeProperties(true);

				typeSettingsProperties.load(typeSettings);

				boolean indexable = GetterUtil.getBoolean(
					typeSettingsProperties.getProperty("indexable"));

				if (indexable) {
					if ((type == ExpandoColumnConstants.STRING) ||
						(type == ExpandoColumnConstants.STRING_ARRAY)) {

						typeSettingsProperties.setProperty(
							ExpandoColumnConstants.INDEX_TYPE,
							String.valueOf(
								ExpandoColumnConstants.INDEX_TYPE_TEXT));
					}
					else {
						typeSettingsProperties.setProperty(
							ExpandoColumnConstants.INDEX_TYPE,
							String.valueOf(
								ExpandoColumnConstants.INDEX_TYPE_KEYWORD));
					}
				}
				else {
					typeSettingsProperties.setProperty(
						ExpandoColumnConstants.INDEX_TYPE,
						String.valueOf(ExpandoColumnConstants.INDEX_TYPE_NONE));
				}

				typeSettingsProperties.remove("indexable");

				updateTypeSettings(columnId, typeSettingsProperties.toString());
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected void updateTypeSettingsSelection() throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection();

			ps = con.prepareStatement(
				"select columnId, typeSettings from ExpandoColumn where " +
					"typeSettings like '%selection%'");

			rs = ps.executeQuery();

			while (rs.next()) {
				long columnId = rs.getLong("columnId");

				String typeSettings = rs.getString("typeSettings");

				typeSettings = typeSettings.replace(
					"selection=1", "display-type=selection-list");
				typeSettings = typeSettings.replace(
					"selection=0", "display-type=text-box");

				updateTypeSettings(columnId, typeSettings);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

}