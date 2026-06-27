<template>
  <v-card flat>
    <v-container>
      <v-row v-if="message">
        <v-col cols="12">
          <v-alert
            dense
            outlined
            dismissible
            transition="scale-transition"
            :type="isError ? 'error' : 'success'"
          >
            {{ message }}
          </v-alert>
        </v-col>
      </v-row>
      <v-row>
        <v-col :loading="loading">
          <v-container>
            <v-row>
              <v-col cols="12" sm="4">
                <v-text-field label="用户 ID" v-model="id"></v-text-field>
              </v-col>
              <v-col cols="12" sm="4">
                <v-btn
                  color="green"
                  dark
                  :disabled="!id"
                  text
                  @click="findUser"
                >
                  搜索
                </v-btn>
              </v-col>
            </v-row>
          </v-container>
          <v-data-table
            :headers="headers"
            :items="userList"
            item-key="id"
            :loading="loading"
            dense
            class="mb-6"
            @click:row="selectUser"
          ></v-data-table>
          <v-form v-model="valid" @submit.prevent="updateUser">
            <v-card v-if="user" class="elevation-5">
              <v-card-text>
                <v-container>
                  <v-row>
                    <v-col cols="12" sm="4">
                      <v-text-field
                        label="用户 ID"
                        v-model="user.id"
                        disabled
                      ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <v-text-field
                        label="用户名"
                        v-model="user.username"
                        disabled
                      ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <v-text-field
                        label="创建时间"
                        v-model="user.createdAt"
                        disabled
                      ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <v-text-field
                        label="更新时间"
                        v-model="user.updatedAt"
                        disabled
                      ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <v-text-field
                        label="姓名"
                        v-model="user.name"
                        :rules="[rules.required]"
                      ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <v-text-field
                        label="邮箱"
                        v-model="user.email"
                        :rules="[rules.required, rules.email]"
                      ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <v-select
                        v-model="user.userRoles"
                        :items="roles"
                        label="角色"
                        multiple
                        :rules="[rules.minLength(1, user.userRoles, '角色')]"
                      ></v-select>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <v-switch
                        v-model="user.enabled"
                        inset
                        label="启用"
                      ></v-switch>
                    </v-col>
                  </v-row>
                </v-container>
              </v-card-text>
              <v-card-actions>
                <v-btn color="green" dark text type="submit" :disabled="!valid">
                  更新
                </v-btn>
                <v-dialog
                  v-model="dialog"
                  width="30vw"
                  overlay-opacity="0.98"
                  class="elevation-20"
                >
                  <template v-slot:activator="{ on, attrs }">
                    <v-btn
                      color="red lighten-2 ml-10"
                      dark
                      v-bind="attrs"
                      v-on="on"
                    >
                      删除
                    </v-btn>
                  </template>
                  <v-card>
                    <v-card-text>
                      确定要删除用户 “{{ user.username }}” 吗？
                    </v-card-text>

                    <v-divider></v-divider>

                    <v-card-actions>
                      <v-spacer></v-spacer>
                      <v-btn color="red" text @click="deleteUser">
                        是
                      </v-btn>
                      <v-btn text @click="dialog = false">
                        否
                      </v-btn>
                    </v-card-actions>
                  </v-card>
                </v-dialog>
              </v-card-actions>
            </v-card>
          </v-form>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>

<script>
import * as userService from "@/service/user";
import * as ruleUtil from "@/util/ruleUtil";
export default {
  name: "FindUsers",
  data() {
    return {
      id: null,
      user: null,
      loading: false,
      roles: ["ADMIN", "USER"],
      rules: ruleUtil.rules,
      message: null,
      dialog: false,
      isError: false,
      valid: false,
      userList: [],
      headers: [
        {
          text: "用户 ID",
          value: "id"
        },
        {
          text: "用户名",
          value: "username"
        },
        {
          text: "姓名",
          value: "name"
        },
        {
          text: "邮箱",
          value: "email"
        },
        {
          text: "角色",
          value: "userRoles"
        },
        {
          text: "启用",
          value: "enabled"
        }
      ]
    };
  },
  methods: {
    getUsers() {
      this.message = null;
      this.loading = true;
      userService
        .getAllUsers()
        .then(data => {
          this.isError = false;
          this.userList = data;
          this.loading = false;
        })
        .catch(err => {
          this.isError = true;
          this.loading = false;
          this.message = err.error_description;
        });
    },
    selectUser(user) {
      this.id = user.id;
      this.user = { ...user };
      this.message = null;
    },
    findUser() {
      this.message = null;
      this.loading = true;
      userService
        .getUser(this.id)
        .then(data => {
          this.isError = false;
          this.user = data;
          this.loading = false;
        })
        .catch(err => {
          this.isError = true;
          this.loading = false;
          this.user = null;
          this.message = err.error_description;
        });
    },
    updateUser() {
      this.message = null;
      this.loading = true;
      userService
        .updateUser(this.user)
        .then(data => {
          this.isError = false;
          this.message = "用户更新成功";
          this.loading = false;
          this.user = data;
          const index = this.userList.findIndex(user => user.id === data.id);
          if (index !== -1) {
            this.userList.splice(index, 1, data);
          }
        })
        .catch(err => {
          this.isError = true;
          this.loading = false;
          this.message = err.error_description;
        });
    },
    deleteUser() {
      this.message = null;
      this.loading = true;
      userService
        .deleteUser(this.user.id)
        .then(() => {
          this.isError = false;
          this.userList = this.userList.filter(
            user => user.id !== this.user.id
          );
          this.user = null;
          this.message = "用户删除成功";
          this.loading = false;
          this.dialog = false;
        })
        .catch(err => {
          this.isError = true;
          this.loading = false;
          this.message = err.error_description;
        });
    }
  },
  mounted() {
    this.getUsers();
  }
};
</script>

<style></style>
