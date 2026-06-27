<template>
  <v-card>
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

      <v-row align="center">
        <v-col cols="12" sm="3" v-if="!showForm">
          <v-btn block color="primary" dark @click="showForm = true">
            <v-icon left>mdi-filter</v-icon>
            查询图书
          </v-btn>
        </v-col>
        <v-col cols="12" v-else>
          <v-form
            @submit.prevent="getBooks"
            :loading="loading"
            v-model="valid"
            ref="searchForm"
          >
            <v-row>
              <v-col cols="12" sm="3">
                <v-text-field
                  label="图书名称"
                  clearable
                  dense
                  v-model="book.name"
                ></v-text-field>
              </v-col>
              <v-col cols="12" sm="3">
                <v-text-field
                  label="作者"
                  clearable
                  dense
                  v-model="book.author"
                ></v-text-field>
              </v-col>
              <v-col cols="12" sm="3">
                <v-select
                  label="分类"
                  clearable
                  dense
                  :items="categories"
                  item-text="name"
                  item-value="id"
                  v-model="book.categoryId"
                ></v-select>
              </v-col>
              <v-col cols="12" sm="3">
                <v-text-field
                  label="ISBN"
                  clearable
                  dense
                  v-model="book.isbn"
                ></v-text-field>
              </v-col>
              <v-col cols="12" sm="3">
                <v-text-field
                  label="出版社"
                  clearable
                  dense
                  v-model="book.publication"
                ></v-text-field>
              </v-col>
              <v-col cols="12" sm="9" class="d-flex align-center">
                <v-btn color="green" text dark :disabled="!valid" type="submit">
                  <v-icon left>mdi-magnify</v-icon>
                  搜索
                </v-btn>
                <v-btn color="blue darken-1" text dark @click="clearForm">
                  <v-icon left>mdi-refresh</v-icon>
                  清空
                </v-btn>
                <span class="caption grey--text ml-4">
                  共 {{ bookList.length }} 本图书
                </span>
              </v-col>
            </v-row>
          </v-form>
        </v-col>
      </v-row>
    </v-container>

    <v-data-table
      :headers="headers"
      :items="bookList"
      class="elevation-20"
      item-key="id"
      :loading="loading"
      fixed-header
      height="50vh"
      :footer-props="{
        'items-per-page-text': '每页图书数'
      }"
    >
      <template v-slot:[`item.summary`]="{ item }">
        <span class="summary-cell">{{ item.summary || "-" }}</span>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
import * as bookService from "@/service/book";

export default {
  name: "UserBooksPage",
  data() {
    return {
      loading: false,
      valid: true,
      message: null,
      isError: false,
      showForm: true,
      categories: [],
      bookList: [],
      book: {
        name: null,
        author: null,
        publication: null,
        isbn: null,
        categoryId: null
      },
      headers: [
        {
          text: "图书名称",
          align: "start",
          value: "name",
          class: "indigo--text darken-4"
        },
        {
          text: "作者",
          value: "author",
          class: "indigo--text darken-4"
        },
        {
          text: "分类",
          value: "category.name",
          class: "indigo--text darken-4"
        },
        {
          text: "ISBN",
          value: "isbn",
          class: "indigo--text darken-4"
        },
        {
          text: "出版社",
          value: "publication",
          class: "indigo--text darken-4"
        },
        {
          text: "出版年份",
          value: "publicationYear",
          class: "indigo--text darken-4"
        },
        {
          text: "页数",
          value: "pages",
          class: "indigo--text darken-4"
        },
        {
          text: "简介",
          value: "summary",
          class: "indigo--text darken-4"
        }
      ]
    };
  },
  methods: {
    getBooks() {
      this.loading = true;
      this.message = null;
      this.isError = false;
      this.showForm = false;
      bookService
        .findAllBooks(this.book)
        .then(data => {
          this.bookList = data;
          this.loading = false;
        })
        .catch(err => {
          this.loading = false;
          this.isError = true;
          this.message = err.error_description || "查询图书失败，请稍后重试。";
        });
    },
    getCategories() {
      bookService
        .findAllCategories()
        .then(data => {
          this.categories = data;
        })
        .catch(err => {
          this.isError = true;
          this.message = err.error_description || "查询分类失败，请稍后重试。";
        });
    },
    clearForm() {
      this.book = {
        name: null,
        author: null,
        publication: null,
        isbn: null,
        categoryId: null
      };
      this.$nextTick(() => {
        if (this.$refs.searchForm) {
          this.$refs.searchForm.resetValidation();
        }
        this.getBooks();
      });
    }
  },
  created() {
    this.getCategories();
    this.getBooks();
  }
};
</script>

<style scoped>
.summary-cell {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
