package com.stylelogic.predictionengine;

import org.jdbi.v3.core.Jdbi;
import ra.common.Tuple3;

import java.io.IOException;
import java.io.*;
import java.sql.*;

import java.util.List;
import java.util.logging.Logger;

public class DAO {

	public static final Logger LOG = Logger.getLogger(DAO.class.getName());

	MiningData mine;
	private final Jdbi jdbi;
	final static int MIN_RATINGS_PER_PREDICTON = 1;

	public DAO(MiningData mine) {
		this.mine = mine;
		this.jdbi = Jdbi.create("jdbc:h2:mem:peDB"); // (H2 in-memory database)
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE item (item_id IDENTITY PRIMARY KEY)").execute());
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE user (user_id IDENTITY PRIMARY KEY)").execute());
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE group (group_id IDENTITY PRIMARY KEY)").execute());
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE user_rating (user_id NUMERIC, item_id NUMERIC, rating NUMERIC)").execute());
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE user_group_membership (user_id NUMERIC, group_id NUMERIC, happiness_percent NUMERIC)").execute());
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE prediction_group (group_id NUMERIC, member_count NUMERIC, member_item_rating_sd NUMERIC, item_rating_n NUMERIC, item_rating_sd NUMERIC, item_rating_mean NUMERIC, predictive_error NUMERIC, group_happiness NUMERIC )").execute());
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE group_rating (group_id NUMERIC, item_id NUMERIC, rating NUMERIC, weight NUMERIC)").execute());
		jdbi.withHandle(handle -> handle.createUpdate("CREATE TABLE group_farg (farg_id NUMERIC, group_id NUMERIC)").execute());
	}

	public Jdbi getJdbi() {
		return jdbi;
	}

	public List<Integer> getItemIds() {
		return jdbi.withHandle(handle -> handle.createQuery("SELECT id FROM item ORDER BY id").mapTo(Integer.class).list());
	}

	public List<Integer> getUserIds() {
		return jdbi.withHandle(handle -> handle.createQuery("SELECT id FROM 'user' ORDER BY id").mapTo(Integer.class).list());
	}

	public List<Integer> getGroupIds() {
		return jdbi.withHandle(handle -> handle.createQuery("SELECT id FROM 'group' ORDER BY id").mapTo(Integer.class).list());
	}

	public List<Tuple3> getUserRatings() {
		return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM user_rating GROUP BY user_id").mapTo(Tuple3.class).list());
	}

	public List<Tuple3> getUserRatingsByUser(int userId) {
		return jdbi.withHandle(handle -> handle.createQuery("SELECT item_id, rating FROM user_rating WHERE user_id = :userId").bind("userId", userId).mapTo(Tuple3.class).list());
	}

	public List<Tuple3> getUserGroupMemberships() {
		return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM user_group_membership ORDER BY user_id").mapTo(Tuple3.class).list());
	}

	public void loadIndicies()
	{
//		int[] itemIDs;
		int	i;
		int g;
//		int	count;

//		try
//		{
//			LOG.info("Quering Item Indicies...");
//			count = jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM item").mapTo(Integer.class).one());
//			ResultSet rs = jdbi.Query(con,"exec usp_DM_Index_item_count");
//			rs.next();
//			count = rs.getInt(1);
//			rs.close();
//			itemIDs = new int[count];
//			rs = jdbi.Query(con,"exec usp_DM_Index_item_select");
//			i=0;
			LOG.info("Initializing Item Indicies...");
//			while (rs.next()) {
//				itemIDs[i++] = rs.getInt(1);
//			}
//			rs.close();
			mine.prepareItemIndex(getItemIds());
//			LOG.info("Completed");

//			LOG.info("Quering User Indicies...");
//			count = jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM user").mapTo(Integer.class).one());
//			rs = jdbi.Query(con,"exec usp_DM_Index_user_count");
//			rs.next();
//			count = rs.getInt(1);
//			rs.close();
//			itemIDs = new int[count];
//			rs = jdbi.Query(con,"exec usp_DM_Index_user_select");
//			i=0;
			LOG.info("Initializing User Indicies...");
//			while (rs.next())
//				itemIDs[i++] = rs.getInt(1);

			mine.prepareUserIndex(getUserIds());
//			LOG.info("Completed");

//			LOG.info("Quering Group Indicies...");
//			rs = jdbi.Query(con,"exec usp_DM_Index_group_count");
//			rs.next();
//			count = rs.getInt(1);
//			rs.close();
//			if (count>kValue) kValue=count;
//			mine.groupCount = kValue;
//			rs = jdbi.Query(con,"exec usp_DM_Index_group_select");
//			itemIDs = new int[count];
//			i=0;
//			while (rs.next())
//				itemIDs[i++] = rs.getInt(1);
			LOG.info("Initializing Group Indicies...");
			mine.prepareGroupIndex(getGroupIds());
//			if (count>kValue) kValue=count;
//			mine.groupCount = kValue;

			LOG.info("Initializing Group ResultIDs overall...");
			rs = jdbi.Query("exec usp_DM_Index_group_FARGID_1_select");
			while (rs.next()) {
				i = rs.getInt(1);
				g = rs.getInt(2);
//				groups[groupIndex.getIndex(g)].value_fargID = i;
				mine.groups[g].valueFargID = i;
			}
			LOG.info("Initializing Group ResultIDs histogram...");
			rs = jdbi.Query(con,"exec usp_DM_Index_group_FARGID_33_select");
			while (rs.next()) {
				i = rs.getInt(1);
				g = rs.getInt(2);
				mine.groups[g].histFargID = i;
			}
//			rs.close();
			LOG.info("Loaded indicies.");
//		}
//		catch(SQLException ex){ System.err.println("LoadIndicies SQLException: " + ex.getMessage());}
	}

//	public boolean loadGroupRatings() {
//		int iIndex;
//		int gIndex;
//
//		int i = 0;
//		int rr;
//		int id;
//		short w;
//		float r;
//
//		try {
//	        LOG.info("Quering Group Reviews-------------------");
//			ResultSet rs = jdbi.Query("exec usp_DM_group_Reviews_select");
//	        LOG.info("Loading Group Reviews-------------------");
//			while  ( rs.next() ) {
//				i++;
//				gIndex = mine.getGroupIndex( rs.getInt("groupID") );
//				iIndex = mine.getItemIndex( rs.getInt("itemID") );
//				r = rs.getFloat("rating");
//				w = rs.getShort("weight");
//
//				mine.groups[gIndex].ratings[iIndex].rating = (long)(r*((long)1<<32));
//				mine.groups[gIndex].ratings[iIndex].weight = w;
//
//				if (i%100 == 0) LOG.info(".");
//			}
//	        LOG.info("Completed-------------------");
//
//			LOG.info("Quering Group Ratings-------------------");
//			rs = jdbi.Query("exec usp_DM_group_Ratings_select");
//			LOG.info("Loading Group Ratings-------------------");
//
//			int k = 0;
//			i=-1;
//			gIndex=0;
//			iIndex=0;
//			short[] ratings = new short[7];
//			while  ( rs.next() ) {
//				k++;
//				if (k % 100 == 0)
//					LOG.info(".");
//				gIndex = mine.getGroupIndex( rs.getInt("groupID") );
//				id = rs.getInt("itemID");
//				iIndex = mine.getItemIndex(id);
//				rr = rs.getInt("rating");
//				w = rs.getShort("weight");
//				if (id != i) {
//					if (i>=0) mine.groups[gIndex].ratings[iIndex].ratings = ratings;
//					i = id;
//					ratings = new short[7];
//					for (int count=0;count<7;count++)
//						ratings[count]=0;
//				}
//				ratings[rr] = w;
//			}
//			if (i>=0)
//				mine.groups[gIndex].ratings[iIndex].ratings = ratings;
//			LOG.info("Completed-------------------");
//
//	        readGroupUserMembership();
//
//			for (i=0; i<mine.getGroupCount() ; i++) {
//				mine.groups[i].dx = GroupInfo.DIRTY_DX+1;
//				mine.groups[i].refresh();
//			}
//
//			LOG.info("Completed-------------------");
//			i=1;
//		}
//		catch(SQLException ex){
//			System.err.println("LoadGroupRatings SQLException: " + ex.getMessage());
//		}
//
//		return (i>0);
//	}


//	public void readGroupUserMembership() {
//		LOG.info("Quering Group User Xref-------------------");
//		ResultSet rs = jdbi.Query("exec usp_DM_user_GroupMembership_select");
//		LOG.info("Loading Group User Xref-------------------");
//		while  ( rs.next() ) {
//			int uIndex = mine.getUserIndex( rs.getInt(1) );
//			int gIndex = mine.getGroupIndex( rs.getInt(2) );
//
//			mine.groups[gIndex].userList[uIndex/8] ^= 1<<uIndex%8;
//			mine.groups[gIndex].memberCount++;
//			if (mine.users[uIndex] != null)
//			{
//				mine.users[uIndex].groupIndex = gIndex;
//				mine.users[uIndex].happiness = rs.getInt(3)/100;
//			}
//		}
//		rs.close();
//	}

	public void loadUserRatings() {
//	public void loadUserRatings(int start, int length) {
		int[] tempItems = new int[mine.getItemCount()];		//init for later use with User Ratings
		byte[] tempRatings = new byte[mine.getItemCount()];	//init for later use with User Ratings

//		LOG.info("Querying User Ratings ("+mine.getUserID(start)+" - "+mine.getUserID(start+length-1)+")--------");
		List<Tuple3> userRatings = jdbi.withHandle(handle -> handle
				.createQuery("SELECT * FROM user_rating GROUP BY user_id")
				.mapTo(Tuple3.class)
				.list());
// 		ResultSet rs = jdbi.Query("exec usp_DM_user_Rating_Select_Range "+mine.getUserID(start)+", "+mine.getUserID(start+length-1));
		LOG.info("Loading User Ratings-------------------");
		long endTime = System.currentTimeMillis();
		long totalTime = endTime;
		int count=0;
		boolean hasMore=rs.next();
		byte[] rating = new byte[4];
		int newu=-1;
		while (hasMore) {
			if (count == 0) newu = rs.getInt(1);
			int i=0;
			int u = newu;
			if (count % 100 == 0) System.out.print(".");
			while (newu==u && hasMore)
			{
				int item = mine.getItemIndex( rs.getInt(2) );
				rating = rs.getBytes(3);
				if (rating[0] > (byte) 0)
				{
					tempItems[i] = item;
					tempRatings[i++] = (byte) (rating[0] & 7);
				}
				hasMore=rs.next();
				if (hasMore) newu = rs.getInt(1);
			}
			int index = mine.getUserIndex( u );
			mine.users[index] = new UserInfo( index+start, u, tempItems, tempRatings, i);
//System.out.print(index);
			count++;
		}
		int currentUserId;
		int lastUserId;
		int item;
		int rating;
		for(Tuple3 userRating : userRatings) {
			currentUserId = (Integer)userRating.first;
			item = mine.getItemIndex((Integer)userRating.second);
			rating = (Integer)userRating.third;
		}
//		LOG.info("---->loaded "+count);
		LOG.info("Loaded "+userRatings.size()+" User Ratings.");
		endTime = System.currentTimeMillis();
		LOG.info("Total Load Time ="+(endTime-totalTime));
		rs.close();
		LOG.info("\nCompleted");
	}

	public void loadUserRatingsByUser(int userIndex) {
		int	i = 0;
		int itemId;
		int	userId = 0;
		int[] tempItems;
		byte[] tempRatings;
		byte[] rating;
		int	count;
		try {
			tempItems = new int[mine.getItemCount()];		//init for later use with User Ratings
			tempRatings = new byte[mine.getItemCount()];	//init for later use with User Ratings

	        itemId = mine.getUserID(userIndex);

//	        System.out.println("Loading User "+id+"'s Ratings-------------------");
			ResultSet rs = jdbi.Query(con,"exec usp_DM_user_Rating_Select_Indiv "+itemId);

			rating = new byte[4];
			while (rs.next()) {
				userId = rs.getInt(1);
				itemId = rs.getInt(2);
				if (!mine.validItemID(itemId)) {
					mine.addItem(itemId);
					System.out.println("Adding new Item "+itemId+" ("+mine.getItemIndex(itemId)+"/"+mine.getItemIndexSize()+")");
					for (int g=0;g<mine.getGroupCount();g++)
						mine.groups[g].addItem(mine.getItemIndex(itemId));
				}
				tempItems[i] = mine.getItemIndex( itemId );
				rating = rs.getBytes(3);
				tempRatings[i++] = (byte) (rating[0] & 7);

			}
			if (i>0) mine.users[userIndex].setRatings(tempItems, tempRatings, i);
			rs.close();
		}
		catch(SQLException ex){ System.err.println("LoadIndividualRatings SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}

	public void updateUserInfo() {
		int	i;
		try {
			for (i=0;i<mine.getUserCount();i++)
				if (mine.users[i] != null)
					if (mine.users[i].modified)
						storeUserMembership(i);
		}
		catch(Exception ex){
			LOG.warning("UpdateUserInfo Exception: " + ex.getMessage());
		}
	}
	public void storeUserMembership(int userIndex) {
		try {
			jdbi.postQuery("exec usp_DM_user_GroupMembership_update "+mine.users[userIndex].userID+", "+mine.users[userIndex].groupIndex+", "+(int)(mine.users[userIndex].happiness*100));
			mine.users[userIndex].modified = false;
		}
		catch(SQLException ex){ System.err.println("StoreUserMembership SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}

	public void storeGroupStats(int i) {
//		float			groupHappiness;
//		double			Member_Item_Rating_SD,Item_Rating_SD,Item_Rating_Mean,Predictive_Error;
//		int				Item_Rating_N;

//		try {
//			System.out.print("Storing Group Stats "+i);
//			System.out.println(" "+i+","+								mine.groups[i].memberCount+","+								mine.groups[i].Member_Item_Rating_SD+","+								mine.groups[i].Item_Rating_N+","+								mine.groups[i].Item_Rating_SD+","+								mine.groups[i].Item_Rating_Mean+","+								mine.groups[i].Predictive_Error+","+								mine.groups[i].groupHappiness);
//			jdbi.postQuery("Exec usp_DM_Prediction_Group_Update " +mine.getGroupID(i)+","+	mine.groups[i].memberCount+","+mine.groups[i].memberItemRatingSD +","+mine.groups[i].itemRatingN +","+mine.groups[i].itemRatingSD +","+mine.groups[i].itemRatingMean +","+mine.groups[i].predictiveError +","+mine.groups[i].groupHappiness);
			jdbi.withHandle(handle -> handle.createUpdate("UPDATE prediction_group SET member_count = :membercount, member_item_rating_sd = :memberItemRatingSD, item_rating_n = :itemRatingN, item_rating_sd = :itemRatingSD, item_rating_mean = :itemRatingMean, predictive_error = :predictiveError, group_happiness = :groupHappiness WHERE group_id = :groupId")
					.bind("memberCount", mine.groups[i].memberCount)
					.bind("memberItemRatingSD", mine.groups[i].memberItemRatingSD)
					.bind("itemRatingN", mine.groups[i].itemRatingN)
					.bind("itemRatingSD", mine.groups[i].itemRatingSD)
					.bind("itemRatingMean", mine.groups[i].itemRatingMean)
					.bind("predictiveError", mine.groups[i].predictiveError)
					.bind("groupHappiness", mine.groups[i].groupHappiness)
					.bind("groupId", mine.getGroupID(i))
					.execute());
//		}
//		catch(SQLException ex){ System.err.println("StoreGroupStats SQLException: " + ex.getMessage());}
	}

	public void storeOverallGroupStats() {

		float groupHappiness = 0;
		double memberItemRatingSD = 0.0;
		double itemRatingSD = 0.0;
		double itemRatingMean = 0.0;
		double predictiveError = 0.0;
		int	itemRatingN = 0;
		int memberCount = 0;
		int groupCount = mine.getGroupCount();

		for (int i=0;i<groupCount;i++) {
			memberCount += mine.groups[i].memberCount;
			groupHappiness += mine.groups[i].groupHappiness;
			memberItemRatingSD += mine.groups[i].memberItemRatingSD;
			itemRatingSD += mine.groups[i].itemRatingSD;
			itemRatingMean += mine.groups[i].itemRatingMean;
			predictiveError += mine.groups[i].predictiveError;
			itemRatingN += mine.groups[i].itemRatingN;
		}

		groupHappiness /= (double) groupCount;
		memberItemRatingSD /= groupCount;
		itemRatingSD /= groupCount;
		itemRatingMean /= groupCount;
		predictiveError /= groupCount;
		itemRatingN /= groupCount;

		int finalMemberCount = memberCount;
		double finalMemberItemRatingSD = memberItemRatingSD;
		int finalItemRatingN = itemRatingN;
		double finalItemRatingSD = itemRatingSD;
		double finalItemRatingMean = itemRatingMean;
		double finalPredictiveError = predictiveError;
		float finalGroupHappiness = groupHappiness;

		jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO prediction_group (group_id, member_count, member_item_rating_sd, item_rating_n, item_rating_sd, item_rating_mean, predictive_error, group_happiness) " +
				"VALUES (-1, :finalMemberCount, :finalMemberItemRatingSD, :finalItemRatingN, :finalItemRatingSD, :finalItemRatingMean, :finalPredictiveError, :finalGroupHappiness)")
				.bind("finalMemberCount", finalMemberCount)
				.bind("finalMemberItemRatingSD", finalMemberItemRatingSD)
				.bind("finalItemRatingN", finalItemRatingN)
				.bind("finalItemRatingSD", finalItemRatingSD)
				.bind("finalItemRatingMean", finalItemRatingMean)
				.bind("finalPredictiveError", finalPredictiveError)
				.bind("finalGroupHappiness", finalGroupHappiness)
				.execute());

//		try {
//			jdbi.postQuery(con,"Exec usp_DM_Prediction_Group_Update -1,"+memberCount+","+memberItemRatingSD+","+itemRatingN+","+itemRatingSD+","+itemRatingMean+","+predictiveError+","+groupHappiness);
//		}
//		catch(SQLException ex){ System.err.println("StoreOverallGroupStats SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}

	public void storeGroupRatings() {
		int	i;
		LOG.info("Storing Group Ratings...");
		try {
			FileWriter foutValue = new FileWriter("tbl_Temp_Field_Analysis_Result_Group__Item_XrefValue.txt");
			FileWriter foutHistogram = new FileWriter("tbl_Temp_Field_Analysis_Result_Group__Item_XrefHistogram.txt");

			for (i=0;i<mine.getGroupCount();i++) {
				if (mine.groups[i].modified) {
					storeGroupStats(i);
					try {
						for (int j = 0; j < mine.groups[i].ratingCount; j++)
							if (mine.groups[i].ratings[j].modified) {
								if (mine.groups[i].ratings[j].weight >= MIN_RATINGS_PER_PREDICTON) {
									foutValue.write(mine.groups[i].valueFargID + "|" + mine.getItemID(j) + "|" + (((float) mine.groups[i].ratings[j].rating / ((long) 1 << 32))) + "|" + mine.groups[i].ratings[j].weight + ";");
									for (int k = 1; k < 7; k++) {
										foutHistogram.write(mine.groups[i].histFargID + "|" + mine.getItemID(j) + "|" + k + "|" + mine.groups[i].ratings[j].ratings[k] + ";");
									}
								} else if (mine.groups[i].ratings[j].weight == MIN_RATINGS_PER_PREDICTON - 1) {
									foutValue.write(mine.groups[i].valueFargID + "|" + mine.getItemID(j) + "|0.0|0;");
									for (int k = 1; k < 7; k++) {
										foutHistogram.write(mine.groups[i].histFargID + "|" + mine.getItemID(j) + "|" + k + "|0;");
									}
								}
								mine.groups[i].ratings[j].modified = false;
							}
						mine.groups[i].modified = false;
					} catch (Exception ex) {
						System.err.println("StoreGroupRatings SQLException: " + ex.getMessage());
						ex.printStackTrace(System.out);
					}

				}
			}
			foutValue.close();
			foutHistogram.close();
		} catch(IOException ex){
			LOG.warning("StoreGroupRatings IOException: " + ex.getMessage());
		}
//		try {
//			jdbi.postQuery("Exec usp_DM_GroupPackage_Execute");
//		}
//		catch(SQLException ex) {
//			System.err.println("StoreGroupRatings SQLException: " + ex.getMessage());
//		}

		storeOverallGroupStats();

        LOG.info("Completed");
	}

	public void storeNewGroup(int i) {
//		try
//		{
//			ResultSet rs = jdbi.Query("select count(*) from tbl_Prediction_Group where id = "+mine.getGroupID(i));
//			rs.next();
//			int count = rs.getInt(1);
			int count = jdbi.withHandle(handle -> handle
					.createQuery("SELECT COUNT(*) FROM prediction_group WHERE group_id = :groupId")
					.bind("groupId", mine.getGroupID(i))
					.mapTo(Integer.class)
					.one());
			if (count == 0) {
		        LOG.info("Storing Group "+i+" in db");
				jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO user_group (id) VALUES (?)").bind("id", i).execute());
 				rs = jdbi.Query("Exec usp_DM_Prediction_Group_Insert "+mine.getGroupID(i));

				rs.next();
				mine.groups[i].valueFargID = rs.getInt(1);
				mine.groups[i].histFargID = rs.getInt(2);
			}
//			rs.close();
//		}
//		catch(Exception ex){
//			LOG.warning("StoreNewGroup SQLException: " + ex.getMessage());
//		}
	}

	private void removeOldGroup(int i)
	{
		try {
	        LOG.info("Removing Group "+i+" in db");
			jdbi.postQuery(con,"Exec usp_DM_Prediction_Group_Delete "+mine.getGroupID(i));
		} catch(Exception ex){
			LOG.warning("RemoveOldGroup SQLException: " + ex.getMessage());
		}
	}


}
