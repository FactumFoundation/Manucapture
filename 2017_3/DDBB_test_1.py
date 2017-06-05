# DDBB test 1
import MySQLdb

host_address = "localhost"
user_name = "root"
user_pass = "factum"
database_name = "BD2"

db = MySQLdb.connect(host=host_address, user=user_name, passwd=user_pass, db=database_name)
print "DB: " + str(db)
cursor = db.cursor(MySQLdb.cursors.DictCursor)
#query = "UPDATE py_app_settings SET image_unique_id="+str(image_unique_id)+" WHERE ID='1'"

query = "UPDATE py_app_settings SET image_unique_id='199999' WHERE id='1'"

print "---------------------------------" 
print "QUERY: " + query
print cursor.execute(query)
result = cursor.fetchall()
print "RESULT: "+str(result)
print "---------------------------------"
db.close()
